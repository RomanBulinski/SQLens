import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SqlAnalyzeRequest } from '../models/sql-analyze-request.model';
import { DiagramResponse } from '../models/diagram-response.model';

@Injectable({ providedIn: 'root' })
export class SqlAnalysisService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/sql/analyze';

  analyze(request: SqlAnalyzeRequest): Observable<DiagramResponse> {
    return this.http.post<DiagramResponse>(this.apiUrl, request);
  }
}
