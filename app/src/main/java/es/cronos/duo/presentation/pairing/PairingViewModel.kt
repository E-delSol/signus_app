package es.cronos.duo.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.cronos.duo.data.repository.QrCodeRepositoryImpl
import es.cronos.duo.data.repository.UserRepositoryImpl
import es.cronos.duo.domain.repository.QrCodeRepository
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.GenerateQrCodeUseCase
import es.cronos.duo.domain.usecase.LinkPartnerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PairingViewModel(
    private val generateQrCodeUseCase: GenerateQrCodeUseCase,
    private val linkPartnerUseCase: LinkPartnerUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PairingState())
    val state: StateFlow<PairingState> = _state.asStateFlow()

    init {
        listenForPairingChanges()
    }

    private fun listenForPairingChanges() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userDocument = FirebaseFirestore.getInstance().collection("users").document(currentUserId)

        userDocument.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val partnerId = snapshot.getString("partnerId")
                if (!partnerId.isNullOrBlank()) {
                    _state.update { it.copy(isPaired = true) }
                }
            }
        }
    }

    fun onGenerateQrClick() {
        viewModelScope.launch {
            val code = generateQrCodeUseCase()
            _state.update { it.copy(uniqueCode = code, showQrCode = true) }
        }
    }

    fun onDismissQr() {
        _state.update { it.copy(showQrCode = false) }
    }

    fun onCodeScanned(code: String) {
        viewModelScope.launch {
            // The listener will automatically update the state, so we just call the use case
            linkPartnerUseCase(code)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val qrCodeRepository: QrCodeRepository = QrCodeRepositoryImpl()
                val userRepository: UserRepository = UserRepositoryImpl()
                val generateUseCase = GenerateQrCodeUseCase(qrCodeRepository)
                val linkUseCase = LinkPartnerUseCase(qrCodeRepository)
                return PairingViewModel(generateUseCase, linkUseCase, userRepository) as T
            }
        }
    }
}