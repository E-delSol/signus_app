package es.cronos.duo.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.Before
import org.junit.Test

class QrCodeRepositoryImplTest {

    private val firestore: FirebaseFirestore = mockk()
    private val auth: FirebaseAuth = mockk()
    private lateinit var qrCodeRepository: QrCodeRepositoryImpl

    @Before
    fun setup() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        qrCodeRepository = QrCodeRepositoryImpl(firestore, auth)
    }

    @Test
    fun `when generateUniqueCode is called then it cleans old sessions and creates a new one`() = runTest {
        // Given
        val userId = "user123"
        val mockFirebaseUser: FirebaseUser = mockk {
            every { uid } returns userId
        }
        every { auth.currentUser } returns mockFirebaseUser

        // Mocking cleanup
        val mockSessionsColl: CollectionReference = mockk()
        val mockQuery: Query = mockk()
        val mockQueryTask: Task<QuerySnapshot> = mockk()
        val mockQuerySnapshot: QuerySnapshot = mockk()
        
        every { firestore.collection("sessions") } returns mockSessionsColl
        every { mockSessionsColl.whereEqualTo("user1Id", userId) } returns mockQuery
        every { mockQuery.get() } returns mockQueryTask
        coEvery { mockQueryTask.await() } returns mockQuerySnapshot
        every { mockQuerySnapshot.iterator() } returns mutableListOf<QueryDocumentSnapshot>().iterator()

        // Mocking creation
        val mockDocRef: DocumentReference = mockk()
        val mockSetTask: Task<Void> = mockk()
        
        every { mockSessionsColl.document(any()) } returns mockDocRef
        every { mockDocRef.set(any()) } returns mockSetTask
        // Para Task<Void>, await() suele devolver null o podemos simular que termina
        coEvery { mockSetTask.await() } returns mockk() 

        // When
        val code = qrCodeRepository.generateUniqueCode()

        // Then
        code.shouldNotBeNull()
    }

    @Test
    fun `given a valid code when linkPartner is called then updates session and users`() = runTest {
        // Given
        val userId = "user2"
        val user1Id = "user1"
        val code = "CODE123"
        val mockFirebaseUser: FirebaseUser = mockk {
            every { uid } returns userId
        }
        every { auth.currentUser } returns mockFirebaseUser

        val mockDocRef: DocumentReference = mockk()
        val mockGetTask: Task<DocumentSnapshot> = mockk()
        val mockSnapshot: DocumentSnapshot = mockk()
        
        every { firestore.collection("sessions").document(code) } returns mockDocRef
        every { mockDocRef.get() } returns mockGetTask
        coEvery { mockGetTask.await() } returns mockSnapshot
        every { mockSnapshot.exists() } returns true
        every { mockSnapshot.getString("user1Id") } returns user1Id

        // Mock updates
        val mockUpdateTask: Task<Void> = mockk()
        every { mockDocRef.update(any<Map<String, Any>>()) } returns mockUpdateTask
        coEvery { mockUpdateTask.await() } returns mockk()

        val mockUsersColl: CollectionReference = mockk()
        val mockUser1Ref: DocumentReference = mockk()
        val mockUser2Ref: DocumentReference = mockk()
        val mockSetTask: Task<Void> = mockk()

        every { firestore.collection("users") } returns mockUsersColl
        every { mockUsersColl.document(user1Id) } returns mockUser1Ref
        every { mockUsersColl.document(userId) } returns mockUser2Ref
        every { mockUser1Ref.set(any(), any()) } returns mockSetTask
        every { mockUser2Ref.set(any(), any()) } returns mockSetTask
        coEvery { mockSetTask.await() } returns mockk()

        // When
        val result = qrCodeRepository.linkPartner(code)

        // Then
        result shouldBeEqualTo true
    }
}
