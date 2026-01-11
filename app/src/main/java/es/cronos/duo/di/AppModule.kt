package es.cronos.duo.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import es.cronos.duo.data.repository.AuthRepositoryImpl
import es.cronos.duo.data.repository.QrCodeRepositoryImpl
import es.cronos.duo.data.repository.UserRepositoryImpl
import es.cronos.duo.domain.repository.AuthRepository
import es.cronos.duo.domain.repository.QrCodeRepository
import es.cronos.duo.domain.repository.UserRepository
import es.cronos.duo.domain.usecase.*
import es.cronos.duo.presentation.login.LoginViewModel
import es.cronos.duo.presentation.pairing.PairingViewModel
import es.cronos.duo.presentation.semaphore.SemaphoreViewModel
import es.cronos.duo.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    // Firebase
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseMessaging.getInstance() }

    // Repositories
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::UserRepositoryImpl) bind UserRepository::class
    singleOf(::QrCodeRepositoryImpl) bind QrCodeRepository::class

    // Use Cases
    factoryOf(::LoginWithEmailUseCase)
    factoryOf(::RegisterWithEmailUseCase)
    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::GenerateQrCodeUseCase)
    factoryOf(::LinkPartnerUseCase)
    factoryOf(::UnlinkPartnerUseCase)
    factoryOf(::ObserveUserUseCase)
    factoryOf(::GetPartnerStatusUseCase)
    factoryOf(::UpdateUserStatusUseCase)
    factoryOf(::GetUserUseCase)

    // ViewModels
    viewModelOf(::LoginViewModel)
    viewModelOf(::PairingViewModel)
    viewModelOf(::SemaphoreViewModel)
    viewModelOf(::SettingsViewModel)
}