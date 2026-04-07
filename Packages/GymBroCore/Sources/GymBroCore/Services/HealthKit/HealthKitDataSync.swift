import Foundation
import SwiftData
import os

/// Syncs HealthKit data into SwiftData cache. Offline-first: always reads from cache,
/// refreshes from HealthKit when available. HealthKit data never leaves device.
public final class HealthKitDataSync {
    private static let logger = Logger(subsystem: "com.gymbro", category: "HealthKitSync")

    private let service: HealthKitService
    private let modelContext: ModelContext
    private let baselineWindowDays: Int

    public init(
        service: HealthKitService,
        modelContext: ModelContext,
        baselineWindowDays: Int = 30
    ) {
        self.service = service
        self.modelContext = modelContext
        self.baselineWindowDays = baselineWindowDays
    }

    // MARK: - Full Sync

    /// Refresh all health metrics from HealthKit and update cached data + baselines.
    /// Safe to call even if HealthKit is unavailable — degrades gracefully.
    public func syncAll() async {
        guard service.isAvailable else {
            Self.logger.info("HealthKit not available — using cached data only")
            return
        }

        let endDate = Date()
        let startDate = Calendar.current.date(
            byAdding: .day,
            value: -baselineWindowDays,
            to: endDate
        ) ?? endDate

        await syncRestingHeartRate(from: startDate, to: endDate)
        await syncHRV(from: startDate, to: endDate)
        await syncSleep(from: startDate, to: endDate)
        await syncActiveEnergy(from: startDate, to: endDate)

        do {
            try modelContext.save()
            Self.logger.info("Health data sync completed")
        } catch {
            Self.logger.error("Failed to save synced health data: \(error.localizedDescription)")
        }
    }

    // MARK: - Individual Syncs

    private func syncRestingHeartRate(from startDate: Date, to endDate: Date) async {
        do {
            let dataPoints = try await service.fetchRestingHeartRate(from: startDate, to: endDate)
            cacheDataPoints(dataPoints, type: .restingHeartRate)
            updateBaseline(for: .restingHeartRate, dataPoints: dataPoints)
        } catch {
            Self.logger.warning("Resting HR sync failed: \(error.localizedDescription)")
        }
    }

    private func syncHRV(from startDate: Date, to endDate: Date) async {
        do {
            let dataPoints = try await service.fetchHeartRateVariability(from: startDate, to: endDate)
            cacheDataPoints(dataPoints, type: .heartRateVariability)
            updateBaseline(for: .heartRateVariability, dataPoints: dataPoints)
        } catch {
            Self.logger.warning("HRV sync failed: \(error.localizedDescription)")
        }
    }

    private func syncSleep(from startDate: Date, to endDate: Date) async {
        do {
            let sleepRecords = try await service.fetchSleepAnalysis(from: startDate, to: endDate)
            cacheSleepRecords(sleepRecords)
            let dataPoints = sleepRecords.map {
                HealthDataPoint(date: $0.date, value: $0.totalMinutes, unit: "min")
            }
            updateBaseline(for: .sleepDuration, dataPoints: dataPoints)
        } catch {
            Self.logger.warning("Sleep sync failed: \(error.localizedDescription)")
        }
    }

    private func syncActiveEnergy(from startDate: Date, to endDate: Date) async {
        do {
            let dataPoints = try await service.fetchActiveEnergy(from: startDate, to: endDate)
            cacheDataPoints(dataPoints, type: .activeEnergy)
        } catch {
            Self.logger.warning("Active energy sync failed: \(error.localizedDescription)")
        }
    }

    // MARK: - Cache Operations

    private func cacheDataPoints(_ dataPoints: [HealthDataPoint], type: HealthMetricType) {
        for point in dataPoints {
            let metric = HealthMetric(
                type: type,
                value: point.value,
                unit: point.unit,
                date: point.date
            )
            modelContext.insert(metric)
        }
    }

    private func cacheSleepRecords(_ records: [SleepRecord]) {
        let encoder = JSONEncoder()
        for record in records {
            let metadata = try? encoder.encode(record.stages)
            let metric = HealthMetric(
                type: .sleepDuration,
                value: record.totalMinutes,
                unit: "min",
                date: record.date,
                metadata: metadata.flatMap { String(data: $0, encoding: .utf8) }
            )
            modelContext.insert(metric)
        }
    }

    // MARK: - Baseline Calculation

    private func updateBaseline(for type: HealthMetricType, dataPoints: [HealthDataPoint]) {
        guard !dataPoints.isEmpty else { return }

        let values = dataPoints.map(\.value)
        let count = values.count
        let mean = values.reduce(0, +) / Double(count)
        let variance = values.map { ($0 - mean) * ($0 - mean) }.reduce(0, +) / Double(count)
        let stdDev = variance.squareRoot()

        let baseline = HealthBaseline(
            type: type,
            averageValue: mean,
            standardDeviation: stdDev,
            sampleCount: count,
            windowDays: baselineWindowDays
        )
        modelContext.insert(baseline)
    }

    // MARK: - Cache Reads

    /// Get the latest cached metrics of a given type, sorted by date descending.
    public func getCachedMetrics(
        type: HealthMetricType,
        limit: Int = 30
    ) -> [HealthMetric] {
        var descriptor = FetchDescriptor<HealthMetric>(
            predicate: #Predicate<HealthMetric> { $0.type == type },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        descriptor.fetchLimit = limit

        do {
            return try modelContext.fetch(descriptor)
        } catch {
            Self.logger.error("Failed to fetch cached metrics: \(error.localizedDescription)")
            return []
        }
    }

    /// Get the most recent baseline for a given metric type.
    public func getBaseline(for type: HealthMetricType) -> HealthBaseline? {
        var descriptor = FetchDescriptor<HealthBaseline>(
            predicate: #Predicate<HealthBaseline> { $0.type == type },
            sortBy: [SortDescriptor(\.updatedAt, order: .reverse)]
        )
        descriptor.fetchLimit = 1

        do {
            return try modelContext.fetch(descriptor).first
        } catch {
            Self.logger.error("Failed to fetch baseline: \(error.localizedDescription)")
            return nil
        }
    }
}
