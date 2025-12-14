# 📐 ARCHITECTURE.md

## 📌 Purpose

This document defines the **mandatory architectural rules** of the application.

➡️ **Any human or AI (including Gemini) MUST read this file before creating, modifying, or refactoring code.**  
➡️ **If a request violates these rules, it must be explicitly reported and not executed without confirmation.**

This file acts as the **technical contract** of the project.

---

## 🧠 Project Overview

- Platform: **Android**
- Language: **Kotlin**
- UI Framework: **Jetpack Compose (mandatory)**
- Project type: **New application**
- Goal: **Fast MVP with solid best practices**
- Architecture: **MVVM + Clean Architecture**
- Modularization: **Not modularized (single module)**

---

## 🏗️ Architectural Principles

- Clear separation of responsibilities
- Unidirectional dependency flow
- Simplicity over overengineering
- Maintainability and testability
- SOLID principles must be respected

---

## 🧱 Layered Architecture

The application is structured into three **strictly separated layers**:

    presentation/
    domain/
    data/


❗ Responsibilities must **never be mixed** across layers.

---

## 🎨 Presentation Layer

### Responsibility
- UI rendering
- UI state management
- Presentation logic
- Interaction with domain via use cases

### Contains
- ViewModels
- UI State holders
- UI Events / Actions
- Jetpack Compose UI

### Rules
- ❌ No business logic
- ❌ No direct access to data layer
- ❌ No Firebase access
- ✅ Communicates only through **UseCases**
- ✅ ViewModels expose immutable state
- ✅ Uses StateFlow
- ✅ UI is fully declarative (Compose only)

---

## 🧠 Domain Layer

### Responsibility
- Core business rules
- Application logic
- Use case orchestration

### Contains
- UseCases
- Domain entities
- Repository interfaces

### Rules
- ❌ No Android dependencies
- ❌ No Compose
- ❌ No Firebase
- ❌ No framework-specific code
- ✅ Pure Kotlin
- ✅ Fully testable
- ✅ Independent of other layers

---

## 💾 Data Layer

### Responsibility
- Data access implementation
- External services integration

### Contains
- Repository implementations
- Remote data sources (Firebase)
- Optional in-memory persistence

### Rules
- ❌ No UI logic
- ❌ No access from presentation
- ✅ Implements domain repository interfaces
- ✅ Firebase usage limited and controlled
- ✅ Minimal persistence only when justified

---

## 🔄 Dependency Flow (MANDATORY)

    Compose UI
    ↓
    ViewModel
    ↓
    UseCase
    ↓
    Repository (interface)
    ↓
    RepositoryImpl
    ↓
    Firebase / DataSource

❌ Shortcuts or layer skipping are not allowed.

---

## 🎛️ State Management

- State is managed using **StateFlow**
- `MutableStateFlow` must be private to ViewModels
- UI observes immutable `StateFlow`
- UI states and events must be modeled using **sealed classes** (mandatory)

---

## 🧭 Navigation

- Navigation is handled in the presentation layer
- UI reacts to state or navigation events emitted by ViewModels
- Domain layer must not know about navigation

---

## 🔐 Privacy & Security

- Maximum privacy is a core requirement
- ❌ No user history stored by default
- ❌ No sensitive data persistence
- ❌ No logging of user-related data
- ✅ Any persistence must be minimal and justified

---

## ☁️ Backend & Data Access

- Backend: **Firebase**
- Firebase usage must be **minimal**
- ❌ ViewModels must never access Firebase directly
- ❌ Domain layer must never reference Firebase
- ✅ Firebase exists only in the data layer

---

## 🧪 Testing

- Test structure must mirror layers:
    - presentation
    - domain
    - data
- Domain layer must be easily testable
- Fakes and mocks are allowed

---

## 🧹 Coding Rules

### Allowed
- Idiomatic Kotlin
- Immutability by default
- Clear and readable code
- Patterns only when they add value

### Forbidden
- ❌ Global singletons
- ❌ God classes
- ❌ Logic inside composables
- ❌ UI accessing data layer
- ❌ Layer coupling or circular dependencies

---

## 🤖 AI-Specific Rules (Gemini)

1. **Read this file before generating or modifying code**
2. **Strictly follow this architecture**
3. **Do not introduce new patterns without justification**
4. **Do not break layer separation**
5. **If a request violates these rules, report it first**
6. **When undefined, choose the simplest clean solution**

---

## 🏁 Final Notes

This architecture prioritizes:
- Fast development
- Long-term maintainability
- Clear responsibilities
- Conscious simplicity

**Simplicity is a design decision, not a limitation.**
