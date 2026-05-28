export interface User {
  firstName: string;
  lastName: string;
  email: string;
  role?: 'USER' | 'SUPERVISOR' | 'ADMIN';
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
