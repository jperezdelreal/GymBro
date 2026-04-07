import Foundation
import HealthKit
import os

/// Production HealthKit implementation using HKHealthStore.
/// Single shared instance — HKHealthStore is thread-safe.
public final class HealthKitManager: HealthKitService, @unchecked Sendable {
    private static let logger = Logger(subsystem: "com.gymbro", category: "HealthKit")

    private let healthStore: HKHealthStore
    private var observerQueries: [HKObserverQuery] = []

    public init() {
        self.healthStore = HKHealthStore()
    }

    // MARK: - Availability

    public var isAvailable: Bool {
        HKHealthStore.isHealthDataAvailable()
    }

    // MARK: - Data Types

    /// Read-only types GymBro needs — request only what we use.
    private var readTypes: Set<HKObjectType> {
        var types: Set<HKObjectType> = []
        if let rhr = HKQuantityType.quantityType(forIdentifier: .restingHeartRate) {
            types.insert(rhr)
        }
        if let hrv = HKQuantityType.quantityType(forIdentifier: .heartRateVariabilitySDNN) {
            types.insert(hrv)
        }
        if let sleep = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) {
            types.insert(sleep)
        }
        if let energy = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
            types.insert(energy)
        }
        return types
    }

    // MARK: - Authorization

    public func requestAuthorization() async throws {
        guard isAvailable else {
            throw HealthKitServiceError.notAvailable
        }

        // Read-only — we don't write health data, so toShare is empty.
        try await healthStore.requestAuthorization(toShare: [], read: readTypes)
        Self.logger.info("HealthKit authorization requested successfully")
    }

    // MARK: - Resting Heart Rate

    public func fetchRestingHeartRate(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [HealthDataPoint] {
        guard isAvailable else { throw HealthKitServiceError.notAvailable }

        guard let rhrType = HKQuantityType.quantityType(forIdentifier: .restingHeartRate) else {
            throw HealthKitServiceError.queryFailed("Resting heart rate type unavailable")
        }

        return try await fetchDailyStatistics(
            for: rhrType,
            from: startDate,
            to: endDate,
            options: .discreteAverage,
            unit: HKUnit.count().unitDivided(by: .minute()),
            unitString: "bpm"
        )
    }

    // MARK: - Heart Rate Variability

    public func fetchHeartRateVariability(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [HealthDataPoint] {
        guard isAvailable else { throw HealthKitServiceError.notAvailable }

        guard let hrvType = HKQuantityType.quantityType(
            forIdentifier: .heartRateVariabilitySDNN
        ) else {
            throw HealthKitServiceError.queryFailed("HRV type unavailable")
        }

        return try await fetchDailyStatistics(
            for: hrvType,
            from: startDate,
            to: endDate,
            options: .discreteAverage,
            unit: HKUnit.secondUnit(with: .milli),
            unitString: "ms"
        )
    }

    // MARK: - Sleep Analysis

    public func fetchSleepAnalysis(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [SleepRecord] {
        guard isAvailable else { throw HealthKitServiceError.notAvailable }

        guard let sleepType = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) else {
            throw HealthKitServiceError.queryFailed("Sleep analysis type unavailable")
        }

        let predicate = HKQuery.predicateForSamples(
            withStart: startDate,
            end: endDate,
            options: .strictStartDate
        )

        let samples: [HKCategorySample] = try await withCheckedThrowingContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: sleepType,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: [NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)]
            ) { _, results, error in
                if let error {
                    continuation.resume(
                        throwing: HealthKitServiceError.queryFailed(error.localizedDescription)
                    )
                    return
                }
                let categorySamples = (results as? [HKCategorySample]) ?? []
                continuation.resume(returning: categorySamples)
            }
            healthStore.execute(query)
        }

        return aggregateSleepByNight(samples)
    }

    // MARK: - Active Energy

    public func fetchActiveEnergy(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [HealthDataPoint] {
        guard isAvailable else { throw HealthKitServiceError.notAvailable }

        guard let energyType = HKQuantityType.quantityType(
            forIdentifier: .activeEnergyBurned
        ) else {
            throw HealthKitServiceError.queryFailed("Active energy type unavailable")
        }

        return try await fetchDailyStatistics(
            for: energyType,
            from: startDate,
            to: endDate,
            options: .cumulativeSum,
            unit: .kilocalorie(),
            unitString: "kcal"
        )
    }

    // MARK: - Background Delivery

    public func enableBackgroundDelivery() async throws {
        guard isAvailable else { throw HealthKitServiceError.notAvailable }

        // Register for HRV updates
        if let hrvType = HKQuantityType.quantityType(forIdentifier: .heartRateVariabilitySDNN) {
            try await enableDelivery(for: hrvType, frequency: .hourly)
        }

        // Register for sleep updates
        if let sleepType = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) {
            try await enableDelivery(for: sleepType, frequency: .hourly)
        }

        // Register for resting HR updates
        if let rhrType = HKQuantityType.quantityType(forIdentifier: .restingHeartRate) {
            try await enableDelivery(for: rhrType, frequency: .hourly)
        }

        Self.logger.info("Background delivery enabled for HRV, sleep, and resting HR")
    }

    public func disableBackgroundDelivery() async throws {
        guard isAvailable else { return }

        for query in observerQueries {
            healthStore.stop(query)
        }
        observerQueries.removeAll()

        try await healthStore.disableAllBackgroundDelivery()
        Self.logger.info("Background delivery disabled")
    }

    // MARK: - Private Helpers

    private func enableDelivery(
        for sampleType: HKSampleType,
        frequency: HKUpdateFrequency
    ) async throws {
        do {
            try await healthStore.enableBackgroundDelivery(for: sampleType, frequency: frequency)
        } catch {
            throw HealthKitServiceError.backgroundDeliveryFailed(error.localizedDescription)
        }

        // Pair with an observer query — always call completionHandler.
        let query = HKObserverQuery(
            sampleType: sampleType,
            predicate: nil
        ) { [weak self] _, completionHandler, error in
            defer { completionHandler() }
            guard error == nil else {
                Self.logger.error(
                    "Observer query error for \(sampleType.identifier): \(error!.localizedDescription)"
                )
                return
            }
            Self.logger.info("New data available for \(sampleType.identifier)")
            _ = self  // prevent premature dealloc
        }
        healthStore.execute(query)
        observerQueries.append(query)
    }

    /// Fetches daily statistics (average or sum) for a quantity type over a date range.
    private func fetchDailyStatistics(
        for quantityType: HKQuantityType,
        from startDate: Date,
        to endDate: Date,
        options: HKStatisticsOptions,
        unit: HKUnit,
        unitString: String
    ) async throws -> [HealthDataPoint] {
        let predicate = HKQuery.predicateForSamples(
            withStart: startDate,
            end: endDate,
            options: .strictStartDate
        )

        let interval = DateComponents(day: 1)
        let anchorDate = Calendar.current.startOfDay(for: startDate)

        let collection: HKStatisticsCollection = try await withCheckedThrowingContinuation { continuation in
            let query = HKStatisticsCollectionQuery(
                quantityType: quantityType,
                quantitySamplePredicate: predicate,
                options: options,
                anchorDate: anchorDate,
                intervalComponents: interval
            )

            query.initialResultsHandler = { _, results, error in
                if let error {
                    continuation.resume(
                        throwing: HealthKitServiceError.queryFailed(error.localizedDescription)
                    )
                    return
                }
                guard let results else {
                    continuation.resume(
                        throwing: HealthKitServiceError.queryFailed("No statistics returned")
                    )
                    return
                }
                continuation.resume(returning: results)
            }

            healthStore.execute(query)
        }

        var dataPoints: [HealthDataPoint] = []
        collection.enumerateStatistics(from: startDate, to: endDate) { statistics, _ in
            let value: Double?
            if options.contains(.cumulativeSum) {
                value = statistics.sumQuantity()?.doubleValue(for: unit)
            } else {
                value = statistics.averageQuantity()?.doubleValue(for: unit)
            }

            if let value {
                dataPoints.append(
                    HealthDataPoint(
                        date: statistics.startDate,
                        value: value,
                        unit: unitString
                    )
                )
            }
        }

        return dataPoints
    }

    /// Groups sleep samples by night — a "night" is the 24h window ending at noon.
    private func aggregateSleepByNight(_ samples: [HKCategorySample]) -> [SleepRecord] {
        let calendar = Calendar.current

        // Group by "sleep night" — date of the sample's end, shifted by -12h to group overnight sleep.
        var nightBuckets: [Date: [HKCategorySample]] = [:]

        for sample in samples {
            let shiftedEnd = calendar.date(byAdding: .hour, value: -12, to: sample.endDate) ?? sample.endDate
            let nightDate = calendar.startOfDay(for: shiftedEnd)
            nightBuckets[nightDate, default: []].append(sample)
        }

        return nightBuckets.sorted { $0.key < $1.key }.compactMap { nightDate, nightSamples in
            var stages = SleepStageBreakdown()

            for sample in nightSamples {
                let minutes = sample.endDate.timeIntervalSince(sample.startDate) / 60.0
                guard let sleepValue = HKCategoryValueSleepAnalysis(rawValue: sample.value) else {
                    continue
                }

                switch sleepValue {
                case .inBed:
                    stages.inBedMinutes += minutes
                case .awake:
                    stages.awakeMinutes += minutes
                case .asleepCore:
                    stages.coreMinutes += minutes
                case .asleepDeep:
                    stages.deepMinutes += minutes
                case .asleepREM:
                    stages.remMinutes += minutes
                case .asleepUnspecified:
                    stages.asleepMinutes += minutes
                @unknown default:
                    stages.asleepMinutes += minutes
                }
            }

            let total = stages.totalSleepMinutes
            guard total > 0 else { return nil }

            return SleepRecord(
                date: nightDate,
                totalMinutes: total,
                stages: stages
            )
        }
    }
}
