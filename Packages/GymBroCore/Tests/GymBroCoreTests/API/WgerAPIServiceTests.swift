import XCTest
@testable import GymBroCore

final class WgerAPIServiceTests: XCTestCase {

    // MARK: - Mock URLProtocol

    private final class MockURLProtocol: URLProtocol {
        nonisolated(unsafe) static var requestHandler: ((URLRequest) throws -> (HTTPURLResponse, Data))?

        override class func canInit(with request: URLRequest) -> Bool { true }
        override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

        override func startLoading() {
            guard let handler = Self.requestHandler else {
                client?.urlProtocolDidFinishLoading(self)
                return
            }

            do {
                let (response, data) = try handler(request)
                client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
                client?.urlProtocol(self, didLoad: data)
                client?.urlProtocolDidFinishLoading(self)
            } catch {
                client?.urlProtocol(self, didFailWithError: error)
            }
        }

        override func stopLoading() {}
    }

    // MARK: - Helpers

    private func makeSession() -> URLSession {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [MockURLProtocol.self]
        return URLSession(configuration: config)
    }

    private func makeExerciseJSON(
        count: Int = 1,
        next: String? = nil,
        results: [[String: Any]]? = nil
    ) -> Data {
        let defaultResults: [[String: Any]] = [
            [
                "id": 1,
                "name": "Bench Press",
                "description": "<p>Lie on a bench and press the bar up.</p>",
                "category": 11,
                "muscles": [3, 4],
                "muscles_secondary": [2],
                "equipment": [1]
            ]
        ]
        let json: [String: Any] = [
            "count": count,
            "next": next as Any,
            "previous": NSNull(),
            "results": results ?? defaultResults
        ]
        return try! JSONSerialization.data(withJSONObject: json)
    }

    private func makeMuscleJSON() -> Data {
        let json: [String: Any] = [
            "results": [
                ["id": 1, "name": "Bizeps", "name_en": "Biceps"],
                ["id": 2, "name": "Deltoid", "name_en": "Shoulders"],
                ["id": 3, "name": "Pectoralis", "name_en": "Chest"],
                ["id": 4, "name": "Trizeps", "name_en": "Triceps"]
            ]
        ]
        return try! JSONSerialization.data(withJSONObject: json)
    }

    private func makeEquipmentJSON() -> Data {
        let json: [String: Any] = [
            "results": [
                ["id": 1, "name": "Barbell"],
                ["id": 2, "name": "Dumbbell"],
                ["id": 3, "name": "Kettlebell"]
            ]
        ]
        return try! JSONSerialization.data(withJSONObject: json)
    }

    private func makeHTTPResponse(url: URL, statusCode: Int = 200) -> HTTPURLResponse {
        HTTPURLResponse(url: url, statusCode: statusCode, httpVersion: nil, headerFields: nil)!
    }

    // MARK: - WgerAPIService Tests

    func testFetchExercisesDecodesCorrectly() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            XCTAssertTrue(url.absoluteString.contains("exercise"))
            return (self.makeHTTPResponse(url: url), self.makeExerciseJSON())
        }

        let (exercises, nextPage) = try await service.fetchExercises()
        XCTAssertEqual(exercises.count, 1)
        XCTAssertEqual(exercises.first?.name, "Bench Press")
        XCTAssertEqual(exercises.first?.wgerId, 1)
        XCTAssertEqual(exercises.first?.categoryId, 11)
        XCTAssertEqual(exercises.first?.primaryMuscleIds, [3, 4])
        XCTAssertEqual(exercises.first?.secondaryMuscleIds, [2])
        XCTAssertEqual(exercises.first?.equipmentIds, [1])
        XCTAssertNil(nextPage)
    }

    func testFetchExercisesHandlesPagination() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            let json = self.makeExerciseJSON(
                count: 100,
                next: "https://wger.de/api/v2/exercise/?language=2&page=3"
            )
            return (self.makeHTTPResponse(url: url), json)
        }

        let (_, nextPage) = try await service.fetchExercises()
        XCTAssertEqual(nextPage, 3)
    }

    func testFetchExercisesThrowsRateLimited() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            return (self.makeHTTPResponse(url: url, statusCode: 429), Data())
        }

        do {
            _ = try await service.fetchExercises()
            XCTFail("Expected rateLimited error")
        } catch let error as WgerAPIService.APIError {
            if case .rateLimited = error {
                // Expected
            } else {
                XCTFail("Expected rateLimited, got \(error)")
            }
        }
    }

    func testFetchExercisesThrowsHTTPError() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            return (self.makeHTTPResponse(url: url, statusCode: 500), Data())
        }

        do {
            _ = try await service.fetchExercises()
            XCTFail("Expected httpError")
        } catch let error as WgerAPIService.APIError {
            if case .httpError(let statusCode) = error {
                XCTAssertEqual(statusCode, 500)
            } else {
                XCTFail("Expected httpError, got \(error)")
            }
        }
    }

    func testFetchExercisesLanguageParam() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            XCTAssertTrue(url.absoluteString.contains("language=2"))
            return (self.makeHTTPResponse(url: url), self.makeExerciseJSON())
        }

        _ = try await service.fetchExercises(language: 2)
    }

    func testFetchMusclesDecodesCorrectly() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            return (self.makeHTTPResponse(url: url), self.makeMuscleJSON())
        }

        let muscles = try await service.fetchMuscles()
        XCTAssertEqual(muscles.count, 4)
        XCTAssertEqual(muscles.first?.name, "Biceps")
        XCTAssertEqual(muscles.first?.id, 1)
    }

    func testFetchMusclesFallsBackToNonEnglishName() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        let json: [String: Any] = [
            "results": [
                ["id": 99, "name": "Trapezius", "name_en": ""]
            ]
        ]
        let data = try! JSONSerialization.data(withJSONObject: json)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            return (self.makeHTTPResponse(url: url), data)
        }

        let muscles = try await service.fetchMuscles()
        XCTAssertEqual(muscles.first?.name, "Trapezius")
    }

    func testFetchEquipmentDecodesCorrectly() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            return (self.makeHTTPResponse(url: url), self.makeEquipmentJSON())
        }

        let equipment = try await service.fetchEquipment()
        XCTAssertEqual(equipment.count, 3)
        XCTAssertEqual(equipment[0].name, "Barbell")
        XCTAssertEqual(equipment[1].name, "Dumbbell")
        XCTAssertEqual(equipment[2].name, "Kettlebell")
    }

    // MARK: - ExerciseSource Enum Tests

    func testExerciseSourceRawValues() {
        XCTAssertEqual(ExerciseSource.seed.rawValue, "seed")
        XCTAssertEqual(ExerciseSource.wger.rawValue, "wger")
        XCTAssertEqual(ExerciseSource.custom.rawValue, "custom")
    }

    func testExerciseSourceCodable() throws {
        let encoder = JSONEncoder()
        let decoder = JSONDecoder()

        for source in [ExerciseSource.seed, .wger, .custom] {
            let data = try encoder.encode(source)
            let decoded = try decoder.decode(ExerciseSource.self, from: data)
            XCTAssertEqual(decoded, source)
        }
    }

    // MARK: - Data Model Tests

    func testWgerExerciseDataProperties() {
        let data = WgerExerciseData(
            wgerId: 42,
            name: "Squat",
            description: "A compound leg exercise",
            categoryId: 9,
            primaryMuscleIds: [10, 7],
            secondaryMuscleIds: [9],
            equipmentIds: [1]
        )

        XCTAssertEqual(data.wgerId, 42)
        XCTAssertEqual(data.name, "Squat")
        XCTAssertEqual(data.categoryId, 9)
        XCTAssertEqual(data.primaryMuscleIds, [10, 7])
        XCTAssertEqual(data.secondaryMuscleIds, [9])
        XCTAssertEqual(data.equipmentIds, [1])
    }

    func testFetchExercisesWithEmptyResults() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            let json = self.makeExerciseJSON(count: 0, results: [])
            return (self.makeHTTPResponse(url: url), json)
        }

        let (exercises, nextPage) = try await service.fetchExercises()
        XCTAssertTrue(exercises.isEmpty)
        XCTAssertNil(nextPage)
    }

    func testFetchExercisesDecodingError() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            let badJSON = "{ invalid json }".data(using: .utf8)!
            return (self.makeHTTPResponse(url: url), badJSON)
        }

        do {
            _ = try await service.fetchExercises()
            XCTFail("Expected decodingError")
        } catch let error as WgerAPIService.APIError {
            if case .decodingError = error {
                // Expected
            } else {
                XCTFail("Expected decodingError, got \(error)")
            }
        }
    }

    func testFetchExercisesPageParam() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            XCTAssertTrue(url.absoluteString.contains("page=5"))
            return (self.makeHTTPResponse(url: url), self.makeExerciseJSON())
        }

        _ = try await service.fetchExercises(page: 5)
    }

    // MARK: - Exercise Image Tests

    private func makeExerciseImageJSON(
        count: Int = 1,
        next: String? = nil,
        results: [[String: Any]]? = nil
    ) -> Data {
        let defaultResults: [[String: Any]] = [
            [
                "id": 100,
                "exercise_base": 1,
                "image": "https://wger.de/media/exercise-images/1/bench-press.jpg",
                "is_main": true
            ]
        ]
        let json: [String: Any] = [
            "count": count,
            "next": next as Any,
            "results": results ?? defaultResults
        ]
        return try! JSONSerialization.data(withJSONObject: json)
    }

    func testFetchExerciseImagesDecodesCorrectly() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            XCTAssertTrue(url.absoluteString.contains("exerciseimage"))
            return (self.makeHTTPResponse(url: url), self.makeExerciseImageJSON())
        }

        let (images, nextPage) = try await service.fetchExerciseImages()
        XCTAssertEqual(images.count, 1)
        XCTAssertEqual(images.first?.id, 100)
        XCTAssertEqual(images.first?.exerciseBaseId, 1)
        XCTAssertEqual(images.first?.imageURL, "https://wger.de/media/exercise-images/1/bench-press.jpg")
        XCTAssertTrue(images.first?.isMain ?? false)
        XCTAssertNil(nextPage)
    }

    func testFetchExerciseImagesHandlesPagination() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            let json = self.makeExerciseImageJSON(
                count: 100,
                next: "https://wger.de/api/v2/exerciseimage/?page=2"
            )
            return (self.makeHTTPResponse(url: url), json)
        }

        let (_, nextPage) = try await service.fetchExerciseImages()
        XCTAssertEqual(nextPage, 2)
    }

    func testFetchExerciseImagesThrowsRateLimited() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            return (self.makeHTTPResponse(url: url, statusCode: 429), Data())
        }

        do {
            _ = try await service.fetchExerciseImages()
            XCTFail("Expected rateLimited error")
        } catch let error as WgerAPIService.APIError {
            if case .rateLimited = error {
                // Expected
            } else {
                XCTFail("Expected rateLimited, got \(error)")
            }
        }
    }

    func testFetchExerciseImagesEmptyResults() async throws {
        let session = makeSession()
        let service = WgerAPIService(session: session)

        MockURLProtocol.requestHandler = { request in
            let url = request.url!
            let json = self.makeExerciseImageJSON(count: 0, results: [])
            return (self.makeHTTPResponse(url: url), json)
        }

        let (images, nextPage) = try await service.fetchExerciseImages()
        XCTAssertTrue(images.isEmpty)
        XCTAssertNil(nextPage)
    }

    func testMuscleImageURLGeneration() {
        let url = WgerAPIService.muscleImageURL(for: 3)
        XCTAssertEqual(url, "https://wger.de/static/images/muscles/main/muscle-3.svg")
    }

    func testWgerExerciseImageDataProperties() {
        let data = WgerExerciseImageData(
            id: 42,
            exerciseBaseId: 7,
            imageURL: "https://wger.de/media/exercise-images/7/squat.jpg",
            isMain: true
        )

        XCTAssertEqual(data.id, 42)
        XCTAssertEqual(data.exerciseBaseId, 7)
        XCTAssertEqual(data.imageURL, "https://wger.de/media/exercise-images/7/squat.jpg")
        XCTAssertTrue(data.isMain)
    }
}
