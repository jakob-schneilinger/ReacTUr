export interface User {
  firstName: string;
  lastName: string;
  email: string;
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
