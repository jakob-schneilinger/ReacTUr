import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { GetUserDto } from '../../../dtos/user.dto';
import { UserService } from '../../../services/user.service';
import { NotificationService } from '../../../services/notification.service';
import { Role } from '../../../dtos/enums/role.enum';
import { TRANSLOCO_IMPORTS } from '../../../global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, TRANSLOCO_IMPORTS],
})
export class UserManagementComponent implements OnInit {
  searchTerm: string = '';

  panels = {
    pending: true,
    active: false,
    deactivated: false,
  };

  allUsers: GetUserDto[] = [];

  pendingUsers: GetUserDto[] = [];
  activeUsers: GetUserDto[] = [];
  deactivatedUsers: GetUserDto[] = [];

  constructor(
    private userService: UserService,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef,
    private transloco: TranslocoService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.authService.getLoggedInUser().subscribe({
      next: value => {
        const user = value;
        if (!user) return;

        this.userService.getAllUsers().subscribe({
          next: (users) => {
            this.allUsers = users.filter((u) => u.id !== user.id);
            this.updateFilteredLists();
          },
          error: (err) =>
            this.notification.error(
              this.transloco.translate('admin.user.notifications.fetchError'),
              err.message || err.error || err,
            ),
        });
      }
    })
  }

  private updateFilteredLists(): void {
    const search = this.searchTerm.toLowerCase();

    const filteredBySearch = this.allUsers.filter((user) => {
      if (!search) return true;

      const firstName = user.firstName?.toLowerCase() || '';
      const lastName = user.lastName?.toLowerCase() || '';

      const fullName = `${firstName} ${lastName}`.trim();
      const reverseName = `${lastName} ${firstName}`.trim();

      return (
        fullName.includes(search) ||
        reverseName.includes(search) ||
        user.email?.toLowerCase().includes(search) ||
        user.role?.toLowerCase().includes(search)
      );
    });

    this.pendingUsers = filteredBySearch.filter((user) => (!user.role || user.role == Role.UNAUTHORIZED ) && !user.deactivatedAt);
    this.activeUsers = filteredBySearch.filter((user) => user.role != Role.UNAUTHORIZED && !user.deactivatedAt);
    this.deactivatedUsers = filteredBySearch.filter((user) => !!user.deactivatedAt);

    this.cdr.markForCheck();
  }

  togglePanel(panel: 'pending' | 'active' | 'deactivated'): void {
    this.panels[panel] = !this.panels[panel];
  }

  updateUserStatus(user: GetUserDto, newStatus: 'ACTIVATED' | 'DEACTIVATED'): void {
    const deactivate = newStatus === 'DEACTIVATED';

    const obs = deactivate
      ? this.userService.archiveUser(user.id)
      : this.userService.restoreUser(user.id);

    obs.subscribe({
      next: () => {
        user.deactivatedAt = deactivate ? new Date().toISOString() : null;

        this.updateFilteredLists();
        this.notification.success(
          this.transloco.translate('admin.user.notifications.statusUpdated'),
        );
      },
      error: (err) => {
        this.notification.error(
          this.transloco.translate('admin.user.notifications.statusError'),
          err.message || err.error || err,
        );
      },
    });
  }

  updateUserRole(user: GetUserDto, event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    const newRole = selectElement.value as Role;

    this.userService.updateUserRole(user.id, newRole).subscribe({
      next: () => {
        user.role = newRole;
        this.updateFilteredLists();
        this.notification.success(this.transloco.translate('admin.user.notifications.roleUpdated'));
      },
      error: (err) => {
        this.notification.error(
          this.transloco.translate('admin.user.notifications.roleError'),
          err.message || err.error || err,
        );
      },
    });
  }

  onSearchChange(value: string): void {
    this.searchTerm = value;
    this.updateFilteredLists();
  }

  clearUserSearch() {
    this.searchTerm = '';
    this.updateFilteredLists();
  }

  protected readonly Role = Role;
}
