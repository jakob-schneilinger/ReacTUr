export interface User {
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

export interface FriendDto extends User{
  requesterEmail: string;
  accepted: boolean;
}
