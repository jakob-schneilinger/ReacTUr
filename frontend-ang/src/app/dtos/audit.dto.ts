import { GetUserDto } from './user.dto';
import { AuditAction } from './enums/audit-action.enum';

export interface AuditLogDto {
  uuid: string;
  experiment: string; //uuid
  user: GetUserDto;
  timestamp: string;
  action: AuditAction;
  changeFrom: string;
  changeTo: string;
}
