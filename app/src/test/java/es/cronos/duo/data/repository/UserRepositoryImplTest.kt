package es.cronos.duo.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import es.cronos.duo.domain.model.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class UserRepositoryImplTest {

    private val firestore: FirebaseFirestore = mockk()
    private val auth: FirebaseAuth = mockk()
    private val fcm: FirebaseMessaging = mockk()
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        userRepository = UserRepositoryImpl(firestore, auth, fcm)
    }

    @Test
    fun `given user is logged in when getUser is called then return user from firestore`() = runTest {
        // Given
        val uid = "user123"
        val mockFirebaseUser: FirebaseUser = mockk {
            every { this@mockk.uid } returns uid
        }
        every { auth.currentUser } returns mockFirebaseUser

        val mockDocumentSnapshot: DocumentSnapshot = mockk {
            every { toObject(User::class.java) } returns User(id = uid, email = "test@example.com")
        }
        val mockTask: Task<DocumentSnapshot> = mockk()
        val mockDocRef: DocumentReference = mockk()
        val mockCollRef: CollectionReference = mockk()

        every { firestore.collection("users") } returns mockCollRef
        every { mockCollRef.document(uid) } returns mockDocRef
        every { mockDocRef.get() } returns mockTask
        coEvery { mockTask.await() } returns mockDocumentSnapshot

        // When
        val result = userRepository.getUser()

        // Then
        result?.id shouldBeEqualTo uid
        result?.email shouldBeEqualTo "test@example.com"
    }

    @Test
    fun `given user is not logged in when getUser is called then return null`() = runTest {
        // Given
        every { auth.currentUser } returns null

        // When
        val result = userRepository.getUser()

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `when firestore fails then getUser propagates the error`() = runTest {
        // Given
        val uid = "user123"
        val mockFirebaseUser: FirebaseUser = mockk {
            every { this@mockk.uid } returns uid
        }
        every { auth.currentUser } returns mockFirebaseUser

        val mockTask: Task<DocumentSnapshot> = mockk()
        val mockDocRef: DocumentReference = mockk()
        val mockCollRef: CollectionReference = mockk()

        every { firestore.collection("users") } returns mockCollRef
        every { mockCollRef.document(uid) } returns mockDocRef
        every { mockDocRef.get() } returns mockTask
        coEvery { mockTask.await() } throws Exception("Firestore error")

        // When & Then
        try {
            userRepository.getUser()
        } catch (e: Exception) {
            e.message shouldBeEqualTo "Firestore error"
        }
    }
}
