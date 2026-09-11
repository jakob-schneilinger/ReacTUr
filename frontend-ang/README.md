# Frontend Laboratory Management System (Angular)

This project is a modern Angular-based frontend application designed for laboratory experiment management. It was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.2.9.

## Key Features

- **Reactant Management**: Track chemical reactants with support for:
  - 2D structure drawing using **ChemDoodle**.
  - 3D structure visualization using **3Dmol.js**.
  - Chemical property calculation and SMILES validation via **RDKit**.
- **Experiment Management**:
  - Create and edit experiments.
  - File upload support for various formats (`.txt`, `.csv`, `.prism`,  etc.).
  - Automatic metadata extraction from uploaded raw `.csv` files.
  - Experiment history logs.
- **Experiment Planning**: Simulate chemical reactions and calculate times for target conversions.
- **Search and Advanced Search**: Search through experiments and reactants with various filters (also with **ChemDoodle**). Additionally, substructure search via **RDKit** is also available.
- **User and Option Management**: Administrators can manage users and selectable options (e.g., instruments, modality, solvents, chemical types).
- **Role-Based Access Control**: Different permissions for Users, Supervisors, and Admins.
- **Internationalization (i18n)**: Multi-language support via **Transloco**. Currently, supports English and a special test language for testing.

## Architecture & Components

The application follows a **Modular Feature-Based Architecture**, emphasizing high cohesion within features and low coupling between them.

### Core Principles
- **Container/Presenter Pattern**: Folders in `app/features/` act as containers (e.g., admin, search, reactants, etc.). Pages in `pages/` subdirectories represent full pages containing different components. Components in `components/` subdirectories are mostly encapsulated small modules, often with a single responsibility.
- **Service-Oriented Data Access**: Most features have a dedicated service (e.g., `ExperimentService`, `ReactantService`) that abstracts API calls and data transformation.
- **Global Infrastructure**: The `app/core/` module provides services (Auth, Notifications) and the main application shell.

### Directory Structure & Key Components

#### Core Module (`src/app/core/`)
- **`Auth`**: Manages OIDC authentication via `AuthService`. Includes `AuthGuard` for protected routes and `RoleGuard` for permission-based access.
- **`Layout`**: Contains `LayoutComponent` (the main shell) and `HeaderComponent`, which represents the Navbar. Also includes `NotificationsComponent` `NotificationService` for handling success and error popup notifications.
- **`Global`**: Application-wide constants and `Transloco` configuration.

#### Feature Modules (`src/app/features/`)
- **Experiments (and My Experiments)**:
  - `ExperimentDetailsPageComponent`: Container for creating and editing experiment data.
  - `MyExperimentsPageComponent`: Overview for personally created and archived experiments.
  - `ExperimentDetailsFormComponent`: Form for experiment metadata.
  - `ExperimentHistoryModalComponent`: Displays audit logs for experiment changes.
  - `ExperimentViewModalComponent`: Component for viewing experiment data in a modal (no editing, view only).
  - `ExtractedMetadataConfirmModalComponent`: Shows extracted experiment metadata for user review.
  - `ExperimentService` and `ExperimentFileService`: Handle interaction with experiment and experiment file endpoints.
- **Reactants**:
  - `ReactantOverviewPageComponent`: Lists reactants with search/filtering capabilities.
  - `ReactantDetailsPageComponent`: Page for creating and editing reactants.
  - `ExperimentDetailsFormComponent`: Form for reactant data.
  - `ReactantViewModalComponent`: Component for viewing reactant data in a modal (no editing, view only).
  - `ReactantStructureComponent`: A component integrating **3Dmol.js** (for 3D visualization) and **RDKit** (for SVG rendering).
  - `ReactantService`: Manages reactant CRUD and chemical property retrieval.
- **Experiment Planning**:
  - `ExperimentPlanningPageComponent`: The interactive kinetics simulator.
  - `PlanningChartComponent`: Visualizes concentration changes over time using **ECharts**.
  - `KineticsCalculatorService`: Pure logic service for second-order reaction calculations.
- **Search**:
  - `SearchPageComponent`: Provides a unified search interface for experiments and reactants.
  - `AdvancedSearchComponent`: Provides advanced search options for experiments and reactants.
  - `ExportModalComponent`: Provides a modal for exporting search results.
  - `SubstructureSearchComponent`: Enables chemical similarity searching using **RDKit**'s substructure matching.
  - `SearchService`: Manages experiment search.
- **Admin**:
  - `AdminPageComponent`: Admin page for option and user management.
  - `OptionManagmentComponent`: Create new and manage existing options (e.g., instruments, solvents, etc.)
  - `UserManagmentComponent`: User and User-Role Management.
- **Login**:
  - `LoginPageComponent`: The login page for OIDC authentication.
  - `UnauthorizedPageComponent`: Page shown when the user is not authorized to access the application.

#### Shared Module (`src/app/shared/`)
- **`Directives`**: Custom directives for drag-and-drop and dynamic hiding of elements based on the users role.
- **`DrawMolComponent`**: A wrapper around **ChemDoodle** for drawing 2D chemical structures. It bi-directionally syncs with SMILES strings using **RDKit**.
- **`FileUploadComponent`**: A robust file uploader supporting drag-and-drop and clipboard paste (`PasteTableComponent`).
- **`Modals`**: Standardized, reusable modal wrappers.
- **`DTOs`**: Data Transfer Objects (DTOs) for API requests and responses (`audit.dto.ts`, `user.dto.ts`, etc.).
- **Shared Services**:
  - `option.service.ts` mainly for administrative handling of options (e.g., instruments, modality, ...).
  - `rdkit.service.ts` which manages the lifecycle of the RDKit module and provides chemical utility functions (SMILES validation, InChIKey generation).
  - `shortcut.service.ts` for keyboard shortcuts.
  - `user.service.ts` mainly for administrative user management.

#### ~~Mock Module (`src/app/mock/`)~~ DEPRECATED
~~- Mock Data: Contains hardcoded datasets (mock-experiments.ts, mock-reactants.ts, etc.) to simulate backend responses, for UI development and testing without requiring a live server connection.~~

#### Global Styling (`src/style.css` and `src/styles/`)
These files handle application-wide styling, CSS variables, and layout overrides to ensure a consistent theme across all features.

### Library Integrations
- **Chemical Intelligence**: **RDKit** is the backbone for SMILES/structure validation and substructure search.
- **2D/3D Rendering**: **ChemDoodle** is used for structure editing, while **3Dmol.js** handles hardware-accelerated 3D molecule viewing.
- **Data Visualization**: **ECharts** is utilized for plotting kinetic reaction timelines.
- **UI Framework**: **Bootstrap 5** combined with **ng-bootstrap** provides responsive components.
- **i18n**: **Transloco** manages runtime translations and dynamic language switching.

The **ChemDoodleWeb** library is managed manually and stored locally in the root `lib/ChemDoodleWeb/` directory rather than through npm. Ensure this folder remains intact when cloning the repository, as it is strictly required for the 2D molecule drawing functionalities.

## Getting Started

### Prerequisites
- [Node.js](https://nodejs.org/) (check `package.json` for version compatibility)
- [Angular CLI](https://angular.dev/tools/cli)

### Installation
```bash
npm install
```

### Environment Configuration
Modify the `src/environments/environment.ts` file if needed.

### Development server
To start a local development server, run:
```bash
ng serve
```
Navigate to `http://localhost:4200/`. The application will automatically reload if you change any source files.

### Building
```bash
ng build
```
The build artifacts will be stored in the `dist/` directory.

## How to Extend

1. **Adding a New Feature**: 
   - Create a new directory in `src/app/features/`.
   - Add your components, pages, and services there.
   - Register new routes in `src/app/app.routes.ts`.
2. **Adding a Shared Component**:
   - Place reusable UI elements in `src/app/shared/`.
3. **Adding a New Language**:
   - Add a new JSON file in `public/i18n/`.
   - Update `app.config.ts` to add new languages or remove existing ones.

## Future Outlook / Missing Features

- **Experiment Folder Structure**: Create experiment folder management system in frontend. Backend connection is already available.
- **Substructure Search on Reactant Page**: Add substructure search functionality to the reactant list and to the experiment creation page.
- **Additional Short Names Search**: Add search for additional short names of reactants.
- **Frontend Testing**: Add unit and integration tests for the frontend components.
- **Enhanced Metadata Extraction**: Expand the parser to support more proprietary laboratory file formats.
- **Real-time Notifications**: Implement WebSockets for live updates on shared experiments.
- **Window Optimization**: Further refinement of the responsive layout for laboratory computers (e.g., half-screen).

---

For more information on using the Angular CLI, visit the [Angular CLI Overview](https://angular.dev/tools/cli) page.
