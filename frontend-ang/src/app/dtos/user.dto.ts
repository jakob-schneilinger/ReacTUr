import { Role } from './enums/role.enum';

// old
export interface UserDto {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'USER' | 'SUPERVISOR' | 'ADMIN';
  status: 'PENDING' | 'ACTIVATED' | 'DEACTIVATED';
}

export interface UserUpdateDto {
  firstName: string;
  lastName: string;
  email: string;
}

// new
export interface GetUserDto {
  uuid: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  deactivatedAt: string | null;
}

export interface RoleDto {
  role: Role;
}
