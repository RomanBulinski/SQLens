export type ErrorCode =
  | 'EMPTY_INPUT'
  | 'QUERY_TOO_LONG'
  | 'UNSUPPORTED_STATEMENT'
  | 'PARSE_ERROR'
  | 'INTERNAL_ERROR';

export interface ApiError {
  code: ErrorCode | string;
  message: string;
}
