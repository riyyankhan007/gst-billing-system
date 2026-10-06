# Architecture Skill

You are working on a production-grade GST billing SaaS.

## Rules

- Follow clear separation of concerns.
- Controllers handle HTTP concerns only.
- Services contain business logic.
- Repositories handle persistence only.
- Entities represent persistence/domain state.
- DTOs should be used for API boundaries where appropriate.
- Do not put business logic inside controllers.
- Do not expose JPA entities directly when doing so creates security or coupling risks.
- Keep GST calculations in dedicated domain/service components.
- Avoid circular dependencies.
- Prefer small, focused classes over large "god" services.
- Use dependency injection through constructors.
- Keep configuration externalized through environment variables.
- Do not introduce unnecessary frameworks or abstractions.

## Before changing architecture

Consider:
1. Existing package structure.
2. Existing service/repository boundaries.
3. Existing authentication and authorization.
4. Existing database relationships.
5. Backward compatibility with existing APIs.

Prefer incremental changes over unnecessary rewrites.
