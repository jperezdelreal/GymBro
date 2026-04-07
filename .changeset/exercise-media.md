---
'@bradygaster/squad-cli': patch
---

Exercise media: add imageURL, videoURL, and muscleImageURL fields to Exercise model. Extend WgerAPIService with exercise image fetching. Sync service now pulls images from wger.de and maps them to exercises. Seed data includes curated YouTube links for top 50 exercises.
