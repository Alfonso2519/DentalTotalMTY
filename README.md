DentalTotalMTY

Aplicación Android para gestión de citas en una clínica dental, desarrollada en Kotlin con Firebase como backend. Soporta dos roles de usuario — paciente y doctor — cada uno con su propio flujo de navegación y funcionalidades.

Funcionalidades

Para pacientes


Registro e inicio de sesión (correo/contraseña o cuenta de Google).
Agendar nuevas citas seleccionando servicio, fecha y horario disponible.
Ver historial y estado de citas (pendiente, confirmada, cancelada, completada).
Consultar información del consultorio.
Gestionar perfil personal.


Para doctores


Dashboard con resumen de actividad.
Agenda con vista de horarios disponibles/ocupados por día.
Gestión de pacientes.
Bloqueo de días completos en la agenda.
Configuración de cuenta.


Generales


Integración con Firebase Cloud Messaging para notificaciones push de recordatorio de citas (implementada a nivel de código; pendiente de pruebas end-to-end en dispositivo).
Recuperación de contraseña.
Sesión persistente entre usos de la app.


Arquitectura

El proyecto sigue una separación por capas inspirada en MVVM:

ui/                  # Activities y Fragments, separados por rol (auth / paciente / doctor)
data/
  model/              # Modelos de datos (Usuario, Cita, Servicio)
  repository/         # Acceso a Firebase Authentication y Firestore
viewmodel/            # ViewModels que exponen estado a la UI
utils/                # Utilidades (sesión, servicio de notificaciones push)


Cita maneja estados (pendiente/confirmada/cancelada/completada), cálculo de slots de horario ocupados según duración del servicio, y bloqueo de días completos.
Usuario distingue entre rol paciente y doctor, controlando qué navegación y funciones ve cada uno.
La navegación usa el componente Navigation de Android con grafos separados para autenticación, paciente y doctor (nav_auth, nav_paciente, nav_doctor).


Tecnologías

CategoríaTecnologíaLenguajeKotlinUIAndroid Views, Navigation Component, FragmentsBackend / Base de datosFirebase Authentication, Cloud FirestoreNotificacionesFirebase Cloud Messaging (implementado, sin pruebas end-to-end)ConcurrenciaKotlin CoroutinesMin SDK / Target SDK26 / 34

Configuración local


Crea un proyecto en Firebase Console.
Habilita Authentication (correo/contraseña y Google) y Firestore Database.
Registra una app Android con el package name com.dental.totalmty y descarga tu propio google-services.json.
Coloca ese archivo en la carpeta app/ del proyecto.
Abre el proyecto en Android Studio y ejecuta sobre un emulador o dispositivo físico (min SDK 26).


Notas

Proyecto académico desarrollado para practicar arquitectura Android por capas, integración con Firebase (Auth + Firestore + Cloud Messaging) y manejo de dos flujos de usuario distintos dentro de una misma app.
