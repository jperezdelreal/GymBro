import Foundation
import os

public actor WgerAPIService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "WgerAPI")
    private let baseURL = "https://wger.de/api/v2"
    private let session: URLSession
    
    private struct WgerExerciseResponse: Codable {
        let count: Int
        let next: String?
        let previous: String?
        let results: [WgerExercise]
    }
    
    private struct WgerExercise: Codable {
        let id: Int
        let name: String
        let description: String
        let category: Int
        let muscles: [Int]
        let musclesSecondary: [Int]
        let equipment: [Int]
        
        enum CodingKeys: String, CodingKey {
            case id, name, description, category, muscles, equipment
            case musclesSecondary = "muscles_secondary"
        }
    }
    
    private struct WgerMuscle: Codable {
        let id: Int
        let name: String
        let nameEn: String
        
        enum CodingKeys: String, CodingKey {
            case id, name
            case nameEn = "name_en"
        }
    }
    
    private struct WgerEquipment: Codable {
        let id: Int
        let name: String
    }
    
    public enum APIError: Error {
        case invalidURL
        case networkError(Error)
        case decodingError(Error)
        case httpError(statusCode: Int)
        case rateLimited
    }
    
    public init(session: URLSession = .shared) {
        self.session = session
    }
    
    public func fetchExercises(language: Int = 2, page: Int? = nil) async throws -> ([WgerExerciseData], nextPage: Int?) {
        var urlString = "\(baseURL)/exercise/?language=\(language)"
        if let page = page {
            urlString += "&page=\(page)"
        }
        
        guard let url = URL(string: urlString) else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 30
        
        do {
            let (data, response) = try await session.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.httpError(statusCode: 0)
            }
            
            switch httpResponse.statusCode {
            case 200:
                let decoder = JSONDecoder()
                let wgerResponse = try decoder.decode(WgerExerciseResponse.self, from: data)
                
                let exercises = wgerResponse.results.map { exercise in
                    WgerExerciseData(
                        wgerId: exercise.id,
                        name: exercise.name,
                        description: exercise.description,
                        categoryId: exercise.category,
                        primaryMuscleIds: exercise.muscles,
                        secondaryMuscleIds: exercise.musclesSecondary,
                        equipmentIds: exercise.equipment
                    )
                }
                
                let nextPage = extractPageNumber(from: wgerResponse.next)
                
                Self.logger.info("Fetched \(exercises.count) exercises from wger.de")
                return (exercises, nextPage)
                
            case 429:
                Self.logger.warning("Rate limited by wger.de API")
                throw APIError.rateLimited
                
            default:
                Self.logger.error("HTTP error: \(httpResponse.statusCode)")
                throw APIError.httpError(statusCode: httpResponse.statusCode)
            }
            
        } catch let error as APIError {
            throw error
        } catch let error as DecodingError {
            Self.logger.error("Decoding error: \(error.localizedDescription)")
            throw APIError.decodingError(error)
        } catch {
            Self.logger.error("Network error: \(error.localizedDescription)")
            throw APIError.networkError(error)
        }
    }
    
    public func fetchMuscles() async throws -> [WgerMuscleData] {
        let urlString = "\(baseURL)/muscle/"
        
        guard let url = URL(string: urlString) else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 30
        
        do {
            let (data, response) = try await session.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.httpError(statusCode: 0)
            }
            
            guard httpResponse.statusCode == 200 else {
                throw APIError.httpError(statusCode: httpResponse.statusCode)
            }
            
            let decoder = JSONDecoder()
            struct Response: Codable {
                let results: [WgerMuscle]
            }
            let muscleResponse = try decoder.decode(Response.self, from: data)
            
            return muscleResponse.results.map { muscle in
                WgerMuscleData(id: muscle.id, name: muscle.nameEn.isEmpty ? muscle.name : muscle.nameEn)
            }
            
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.networkError(error)
        }
    }
    
    public func fetchEquipment() async throws -> [WgerEquipmentData] {
        let urlString = "\(baseURL)/equipment/"
        
        guard let url = URL(string: urlString) else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 30
        
        do {
            let (data, response) = try await session.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.httpError(statusCode: 0)
            }
            
            guard httpResponse.statusCode == 200 else {
                throw APIError.httpError(statusCode: httpResponse.statusCode)
            }
            
            let decoder = JSONDecoder()
            struct Response: Codable {
                let results: [WgerEquipment]
            }
            let equipmentResponse = try decoder.decode(Response.self, from: data)
            
            return equipmentResponse.results.map { eq in
                WgerEquipmentData(id: eq.id, name: eq.name)
            }
            
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.networkError(error)
        }
    }
    
    // MARK: - Exercise Images
    
    private struct WgerExerciseImageResponse: Codable {
        let count: Int
        let next: String?
        let results: [WgerExerciseImage]
    }
    
    private struct WgerExerciseImage: Codable {
        let id: Int
        let exerciseBase: Int
        let image: String
        let isMain: Bool
        
        enum CodingKeys: String, CodingKey {
            case id
            case exerciseBase = "exercise_base"
            case image
            case isMain = "is_main"
        }
    }
    
    /// Fetches exercise images from wger.de, returning only main images.
    public func fetchExerciseImages(page: Int? = nil) async throws -> ([WgerExerciseImageData], nextPage: Int?) {
        var urlString = "\(baseURL)/exerciseimage/?format=json&is_main=True"
        if let page = page {
            urlString += "&page=\(page)"
        }
        
        guard let url = URL(string: urlString) else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 30
        
        do {
            let (data, response) = try await session.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.httpError(statusCode: 0)
            }
            
            switch httpResponse.statusCode {
            case 200:
                let decoder = JSONDecoder()
                let imageResponse = try decoder.decode(WgerExerciseImageResponse.self, from: data)
                
                let images = imageResponse.results.map { img in
                    WgerExerciseImageData(
                        id: img.id,
                        exerciseBaseId: img.exerciseBase,
                        imageURL: img.image,
                        isMain: img.isMain
                    )
                }
                
                let nextPage = extractPageNumber(from: imageResponse.next)
                Self.logger.info("Fetched \(images.count) exercise images from wger.de")
                return (images, nextPage)
                
            case 429:
                Self.logger.warning("Rate limited by wger.de API (images)")
                throw APIError.rateLimited
                
            default:
                Self.logger.error("HTTP error (images): \(httpResponse.statusCode)")
                throw APIError.httpError(statusCode: httpResponse.statusCode)
            }
        } catch let error as APIError {
            throw error
        } catch let error as DecodingError {
            Self.logger.error("Decoding error (images): \(error.localizedDescription)")
            throw APIError.decodingError(error)
        } catch {
            Self.logger.error("Network error (images): \(error.localizedDescription)")
            throw APIError.networkError(error)
        }
    }
    
    /// Builds a muscle diagram SVG URL for a given wger muscle ID.
    public static func muscleImageURL(for wgerMuscleId: Int) -> String {
        "https://wger.de/static/images/muscles/main/muscle-\(wgerMuscleId).svg"
    }
    
    private func extractPageNumber(from urlString: String?) -> Int? {
        guard let urlString = urlString,
              let url = URL(string: urlString),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let pageParam = components.queryItems?.first(where: { $0.name == "page" }),
              let pageString = pageParam.value,
              let page = Int(pageString) else {
            return nil
        }
        return page
    }
}

public struct WgerExerciseData {
    public let wgerId: Int
    public let name: String
    public let description: String
    public let categoryId: Int
    public let primaryMuscleIds: [Int]
    public let secondaryMuscleIds: [Int]
    public let equipmentIds: [Int]
}

public struct WgerMuscleData {
    public let id: Int
    public let name: String
}

public struct WgerEquipmentData {
    public let id: Int
    public let name: String
}

public struct WgerExerciseImageData {
    public let id: Int
    public let exerciseBaseId: Int
    public let imageURL: String
    public let isMain: Bool
}
