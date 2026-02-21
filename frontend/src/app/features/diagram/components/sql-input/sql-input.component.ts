import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-sql-input',
  standalone: true,
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './sql-input.component.html',
  styleUrl: './sql-input.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SqlInputComponent {
  @Input() loading = false;
  @Output() readonly analyze = new EventEmitter<string>();

  readonly maxLength = 100_000;
  readonly sqlControl = new FormControl('', [
    Validators.required,
    Validators.maxLength(this.maxLength),
  ]);

  get charCount(): number {
    return this.sqlControl.value?.length ?? 0;
  }

  onAnalyze(): void {
    if (this.sqlControl.valid && this.sqlControl.value?.trim()) {
      this.analyze.emit(this.sqlControl.value.trim());
    }
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.ctrlKey && event.key === 'Enter') {
      this.onAnalyze();
    }
  }
}
