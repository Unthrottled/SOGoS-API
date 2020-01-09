export class NoResultsError extends Error {

}

export class RequestError extends Error {
  private code: number;

  constructor(message: string, code: number) {
    super(message);
    this.code = code;
  }
}
