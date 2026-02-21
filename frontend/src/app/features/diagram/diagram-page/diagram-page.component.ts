import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiError } from '../../../core/models/api-error.model';
import { AnalysisState, DiagramStateService } from '../../../core/services/diagram-state.service';
import { SqlAnalysisService } from '../../../core/services/sql-analysis.service';
import { DiagramCanvasComponent } from '../components/diagram-canvas/diagram-canvas.component';
import { ErrorDisplayComponent } from '../components/error-display/error-display.component';
import { SqlInputComponent } from '../components/sql-input/sql-input.component';

@Component({
  selector: 'app-diagram-page',
  standalone: true,
  imports: [
    SqlInputComponent,
    DiagramCanvasComponent,
    ErrorDisplayComponent,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
  templateUrl: './diagram-page.component.html',
  styleUrl: './diagram-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DiagramPageComponent implements OnInit {
  private readonly stateService = inject(DiagramStateService);
  private readonly sqlService = inject(SqlAnalysisService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = toSignal(this.stateService.state$, {
    initialValue: { status: 'idle' } as AnalysisState,
  });

  protected readonly isLoading = computed(() => this.state().status === 'loading');

  protected readonly diagram = computed(() => {
    const s = this.state();
    return s.status === 'success' ? s.data : null;
  });

  protected readonly error = computed(() => {
    const s = this.state();
    return s.status === 'error' ? s.error : null;
  });

  ngOnInit(): void {
    this.stateService.reset();
  }

  onAnalyze(sql: string): void {
    this.stateService.setLoading();
    this.sqlService.analyze({ sql }).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: data => this.stateService.setSuccess(data),
      error: err => {
        const apiError: ApiError = err?.error ?? {
          code: 'INTERNAL_ERROR',
          message: 'An unexpected error occurred. Please try again.',
        };
        this.stateService.setError(apiError);
      },
    });
  }
}
