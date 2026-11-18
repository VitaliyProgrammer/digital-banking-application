/** The bank's error body (RFC 9457 problem details), already in the client's language. */
interface ProblemDetail {
  detail?: string;
  code?: string;
  errors?: Record<string, string>;
}

export const NETWORK_ERROR_CODE = 'network';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }

  static async fromResponse(response: Response): Promise<ApiError> {
    const problem = await readProblem(response);
    return new ApiError(
      response.status,
      problem.code ?? 'unexpected',
      problem.detail ?? response.statusText,
      problem.errors ?? {},
    );
  }

  static network(): ApiError {
    return new ApiError(0, NETWORK_ERROR_CODE, 'The bank could not be reached');
  }
}

async function readProblem(response: Response): Promise<ProblemDetail> {
  try {
    return (await response.json()) as ProblemDetail;
  } catch {
    return {};
  }
}
