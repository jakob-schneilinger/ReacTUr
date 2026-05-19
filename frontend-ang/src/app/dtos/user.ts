export interface User {
  firstName: string;
  lastName: string;
  email: string;
  role?: 'USER' | 'REVIEWER' | 'ADMIN';
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
