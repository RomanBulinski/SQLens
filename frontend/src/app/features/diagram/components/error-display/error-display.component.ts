import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ApiError } from '../../../../core/models/api-error.model';

@Component({
  selector: 'app-error-display',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './error-display.component.html',
  styleUrl: './error-display.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorDisplayComponent {
  @Input() error!: ApiError;
}
