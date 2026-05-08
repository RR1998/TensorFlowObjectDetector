## In order this project to work with cloud you should run the following set up on yout Cloud Run enviroment

# Cloud Run Setup

This backend exposes a small HTTP API used by the Android app to call Gemini through Vertex AI.

The Android app does **not** call Gemini directly and does **not** store a Gemini API key. Instead, the app calls this backend:

```text
Android App → Cloud Run Backend → Vertex AI Gemini
```

## Architecture

```text
Android Jetpack Compose App
   |
   | POST /chat
   v
Cloud Run Service
   |
   | Authenticates with Google Cloud Service Account
   v
Vertex AI Gemini
```

## GCP Services Used

| Service | Purpose |
|---|---|
| Cloud Run | Hosts the backend API |
| Cloud Build | Builds the service from source |
| Artifact Registry | Stores the container image |
| Vertex AI | Provides Gemini model access |
| IAM Service Account | Allows Cloud Run to call Vertex AI |

## 1. Prerequisites

Install and authenticate the Google Cloud CLI:

```bash
gcloud auth login
```

Set the project:

```bash
gcloud config set project smart-image-chat-backend
```

Verify:

```bash
gcloud config get-value project
```

## 2. Enable Required APIs

```bash
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable aiplatform.googleapis.com
```

These are required for Cloud Run deployment, source builds, image storage, and Vertex AI/Gemini access.

## 3. Create the Cloud Run Service Account

This service account is used by the Cloud Run backend at runtime.

```bash
gcloud iam service-accounts create smart-image-chat-runner \
  --display-name="Smart Image Chat Cloud Run Runner"
```

Expected service account:

```text
smart-image-chat-runner@smart-image-chat-backend.iam.gserviceaccount.com
```

## 4. Grant Vertex AI Access

```bash
gcloud projects add-iam-policy-binding smart-image-chat-backend \
  --member="serviceAccount:smart-image-chat-runner@smart-image-chat-backend.iam.gserviceaccount.com" \
  --role="roles/aiplatform.user"
```

This allows the Cloud Run backend to call Gemini through Vertex AI.

## 5. Deploy to Cloud Run

From the backend project root, where `Dockerfile`, `build.gradle.kts`, and `settings.gradle.kts` are located:

```bash
gcloud run deploy smart-image-chat-api \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --service-account smart-image-chat-runner@smart-image-chat-backend.iam.gserviceaccount.com \
  --set-env-vars GOOGLE_CLOUD_PROJECT=smart-image-chat-backend,VERTEX_AI_LOCATION=us-central1,GEMINI_MODEL=gemini-2.5-flash-lite \
  --max-instances 1
```

This deploys the backend to Cloud Run and limits the service to one max instance to reduce unexpected cost during testing.

## 6. Get the Cloud Run URL

```bash
gcloud run services describe smart-image-chat-api \
  --region us-central1 \
  --format="value(status.url)"
```

Example result:

```text
https://smart-image-chat-api-xxxxx-uc.a.run.app
```

Use this as the Android app backend base URL.

## 7. Test the Health Endpoint

```bash
curl https://YOUR_CLOUD_RUN_URL/
```

Expected response:

```text
Smart Image Chat API is running with Gemini
```

## 8. Test the Chat Endpoint

```bash
curl -X POST "https://YOUR_CLOUD_RUN_URL/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "The local model detected a sunflower with 87% confidence. Explain the result.",
    "conversationId": "test-1"
  }'
```

Expected response shape:

```json
{
  "answer": "The model likely identified a sunflower because...",
  "conversationId": "test-1"
}
```

## 9. Android App Integration

The Android app should call the Cloud Run backend, not Gemini directly.

### Retrofit Base URL

```kotlin
private const val BASE_URL = "https://YOUR_CLOUD_RUN_URL/"
```

The trailing `/` is required.

### Retrofit Service

```kotlin
interface CloudRunChatApiService {

    @POST("chat")
    suspend fun sendMessage(
        @Body request: ChatBackendRequest
    ): ChatBackendResponse
}
```

### Request/Response DTOs

```kotlin
data class ChatBackendRequest(
    val message: String,
    val image: ChatBackendImage? = null,
    val conversationId: String? = null
)

data class ChatBackendImage(
    val mimeType: String,
    val base64Data: String
)

data class ChatBackendResponse(
    val answer: String,
    val conversationId: String? = null
)
```

The Android app should no longer contain:

```text
Gemini API key
Gemini model names
Gemini endpoint
x-goog-api-key header
Gemini request/response DTOs
Model fallback logic
```

Those belong in the backend.

## 10. View Logs

Read recent logs:

```bash
gcloud run services logs read smart-image-chat-api \
  --region us-central1 \
  --limit 50
```

Tail live logs:

```bash
gcloud run services logs tail smart-image-chat-api \
  --region us-central1
```

You can also view them in:

```text
Google Cloud Console
→ Cloud Run
→ smart-image-chat-api
→ Logs
```

## 11. Keep Service Ready but Idle

To keep the service deployed but allow it to scale down:

```bash
gcloud run services update smart-image-chat-api \
  --region us-central1 \
  --min-instances 0 \
  --max-instances 1
```

## 12. Disable Service Until Demo Day

To stop serving traffic without deleting the service:

```bash
gcloud run services update smart-image-chat-api \
  --region us-central1 \
  --scaling=0
```

To re-enable for a demo:

```bash
gcloud run services update smart-image-chat-api \
  --region us-central1 \
  --scaling=auto \
  --min-instances 0 \
  --max-instances 1
```

## 13. Optional: Block Public Access

If the service should not be publicly callable while not in use:

```bash
gcloud run services remove-iam-policy-binding smart-image-chat-api \
  --region us-central1 \
  --member="allUsers" \
  --role="roles/run.invoker"
```

Before demo day, allow public access again:

```bash
gcloud run services add-iam-policy-binding smart-image-chat-api \
  --region us-central1 \
  --member="allUsers" \
  --role="roles/run.invoker"
```

## 14. Common Issues

### Container failed to start on port 8080

Make sure the backend reads the `PORT` environment variable:

```kotlin
val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
```

### Missing project environment variable

Deploy with:

```bash
--set-env-vars GOOGLE_CLOUD_PROJECT=smart-image-chat-backend
```

### Permission denied calling Vertex AI

Verify the Cloud Run runtime service account has:

```text
roles/aiplatform.user
```

### Android request fails

Check:

```text
1. Retrofit base URL ends with /
2. Endpoint is POST /chat
3. Cloud Run service is public or Android has auth
4. Request body matches ChatBackendRequest
5. Cloud Run logs show the incoming request
```

## 15. Cost Control Notes

For testing/demo usage:

```text
Use Gemini Flash Lite
Set maxOutputTokens low in the backend
Keep Cloud Run max instances at 1
Do not send full chat history unless needed
Avoid logging base64 image data
Scale to 0 when not using the service
```

Recommended environment variable:

```bash
GEMINI_MODEL=gemini-2.5-flash-lite
```

Recommended backend generation config:

```kotlin
put("maxOutputTokens", 256)
```
