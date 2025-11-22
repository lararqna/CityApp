import com.google.firebase.Timestamp


data class Review(
    val userId: String = "",
    val username: String = "",
    val rating: Float = 0f,
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
