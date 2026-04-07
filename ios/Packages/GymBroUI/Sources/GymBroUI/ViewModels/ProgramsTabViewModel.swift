import Foundation
import SwiftData
import Observation
import GymBroCore
import os

@MainActor
@Observable
public final class ProgramsTabViewModel {
    private static let logger = Logger(subsystem: "com.gymbro", category: "ProgramsTab")

    private let modelContext: ModelContext

    public private(set) var programs: [Program] = []
    public private(set) var activeProgram: Program?
    public private(set) var compliancePercentage: Double = 0
    public var errorMessage: String?

    public init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    public func loadPrograms() {
        do {
            let descriptor = FetchDescriptor<Program>(
                sortBy: [SortDescriptor(\.name)]
            )
            programs = try modelContext.fetch(descriptor)
            activeProgram = programs.first(where: { $0.isActive })

            if let active = activeProgram {
                calculateCompliance(for: active)
            }
        } catch {
            Self.logger.error("Failed to fetch programs: \(error.localizedDescription)")
            errorMessage = "Failed to load programs"
        }
    }

    public func startProgram(_ program: Program) {
        // Deactivate any currently active program
        if let current = activeProgram {
            current.isActive = false
            current.startDate = nil
            current.updatedAt = Date()
        }

        program.isActive = true
        program.startDate = Date()
        program.updatedAt = Date()

        do {
            try modelContext.save()
            activeProgram = program
            calculateCompliance(for: program)
            Self.logger.info("Started program: \(program.name)")
        } catch {
            Self.logger.error("Failed to start program: \(error.localizedDescription)")
            errorMessage = "Failed to start program"
        }
    }

    public func stopProgram() {
        guard let active = activeProgram else { return }

        active.isActive = false
        active.startDate = nil
        active.updatedAt = Date()

        do {
            try modelContext.save()
            activeProgram = nil
            compliancePercentage = 0
            Self.logger.info("Stopped program: \(active.name)")
        } catch {
            Self.logger.error("Failed to stop program: \(error.localizedDescription)")
            errorMessage = "Failed to stop program"
        }
    }

    public func seedProgramsIfNeeded() async {
        do {
            try await ProgramSeeder.seedPrograms(modelContext: modelContext)
            loadPrograms()
        } catch {
            Self.logger.error("Failed to seed programs: \(error.localizedDescription)")
        }
    }

    private func calculateCompliance(for program: Program) {
        guard let startDate = program.startDate else {
            compliancePercentage = 0
            return
        }

        let programWorkouts = program.workouts
        compliancePercentage = ProgramComplianceService.calculateProgramCompliance(
            workouts: programWorkouts,
            program: program,
            startDate: startDate
        )
    }
}
