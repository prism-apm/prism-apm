---
created: 2026-07-20 15:41:40
modified: 2026-07-21 17:17:08
---

# Feature Description

Admin permissions are used to maintain accounts, roles, permissions, and project access scopes in APM UI. Administrators can use the System Management Center to create users, grant roles, group projects, and revoke access.

The System Management Center contains three entry points:

- Permission management: maintains users, roles, and permissions.
- Access assignment: maintains relationships between users and roles, roles and permissions, and users and project groups.
- Project management: maintains projects, project groups, and project ownership relationships.

Admin permissions mainly control two things:

- Whether a user can enter specific features or perform specific management operations.
- Whether a user can view data for specific projects or services.

# Use Cases

Use admin permissions when different users in a team need different access scopes.

Common scenarios include:

- Creating an APM account for a new user.
- Creating roles such as administrator, operations, or developer.
- Controlling which roles can access management features.
- Restricting ordinary users to only the projects or services they own.
- Removing role or project-group relationships when a user leaves or changes responsibilities.

# Permission Relationship Model

When a user accesses APM, both role permissions and project permissions apply.

- User: an account that logs in to APM UI.
- Role: a set of functional permissions, such as an administrator role or a read-only viewer role.
- Permission: the functional scope that a role can access. In the UI, permissions are maintained through a permission name and permission URL.
- Project: a project or service in APM. The project name usually corresponds to a service name.
- Project group: a set of projects that can be granted to users in batches.

You can understand the authorization result through these two chains:

```text
User -> Role -> Permission -> Available features
User -> Project group -> Project -> Visible data
```

Assigning only a role to a user does not guarantee that the user can see project data. The user must also be added to a project group that contains the target project.

# Usage

To enter the management center:

1. Log in to APM UI.
2. Open `http://localhost:8066/#/admin/dashboard`.
3. Enter Permission Management, Access Assignment, or Project Management as needed.

# Permission Management

The Permission Management page contains three tabs:

- User management: add, edit, and delete users. The user list shows ID, username, enabled state, creation time, and action buttons.
- Role management: add, edit, and delete roles. The role list shows ID, role name, creation time, and action buttons.
- Permission management: add, edit, and delete permissions. A permission consists of a permission name and a permission URL, for example `/api/admin/**`.

Before deleting a user, role, or permission, remove its related assignment relationships first. Otherwise the backend rejects the delete operation to avoid dangling authorization records.

# Access Assignment

The Access Assignment page contains three tabs:

- User-role assignment: select a user and a role to create a user-to-role relationship.
- Role-permission assignment: select a role and a permission to create a role-to-permission relationship.
- Project-group user assignment: select a project group and a user to control which project-group data the user can view.

Each assignment tab supports viewing existing relationships, refreshing the list, and removing relationships.

# Project Management

The Project Management page contains three tabs:

- Project management: add, edit, and delete projects. A project contains a project name and display name.
- Project-group management: add, edit, and delete project groups.
- Project-group project assignment: select a project group and a project to create a project ownership relationship.

The project name usually corresponds to the service name in APM data. Ordinary users can only view projects in assigned project groups. The `ADMIN` user can view all projects.

# Recommended Authorization Flow

When configuring an ordinary user for the first time, use this order:

1. Create the project in Project Management.
2. Create the project group in Project Management.
3. Add the project to the project group in Project-group Project Assignment.
4. Create a role in Permission Management, or confirm that an existing role is suitable.
5. Create permissions in Permission Management, or confirm that suitable permissions already exist.
6. Assign permissions to the role in Access Assignment.
7. Create the user in Permission Management.
8. Assign the role to the user in Access Assignment.
9. Add the user to the project group in Access Assignment.
10. Log in as that user and verify accessible features and project data.

If a user only has functional permissions but no project group, the user may be able to enter related features but see no target project data in service lists or service details.

# Notes

- Permission changes may not take effect immediately. After adding, changing, or removing role permissions, wait up to about 60 seconds before asking the user to retry.
- The Refresh button on the page refreshes the current list data. To verify whether a user has received access, switch to that user and log in again, or refresh the business page.
- Before deleting a user, role, permission, project, or project group, remove existing assignment relationships first.
- Ordinary users need both functional permissions and project permissions to view the corresponding service data.

# Troubleshooting

If access is unexpectedly denied, check in this order:

- Whether the user is logged in and whether the login state has expired.
- Whether the user is enabled.
- Whether the user has the correct role.
- Whether the role has the corresponding functional permission.
- Whether about 60 seconds have passed after a permission change.
- Whether the ordinary user has joined a project group containing the target project.
- Whether the target project name matches the service name in APM data.

If even an administrator cannot access management features, confirm that the `ADMIN` role exists in the system and that the current user has that role.
