import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { DiagramResponse } from '../models/diagram-response.model';
import { ApiError } from '../models/api-error.model';

export type AnalysisState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: DiagramResponse }
  | { status: 'error'; error: ApiError };

@Injectable({ providedIn: 'root' })
export class DiagramStateService {
  private readonly _state$ = new BehaviorSubject<AnalysisState>({ status: 'idle' });
  readonly state$ = this._state$.asObservable();

  setLoading(): void {
    this._state$.next({ status: 'loading' });
  }

  setSuccess(data: DiagramResponse): void {
    this._state$.next({ status: 'success', data });
  }

  setError(error: ApiError): void {
    this._state$.next({ status: 'error', error });
  }

  reset(): void {
    this._state$.next({ status: 'idle' });
  }
}
