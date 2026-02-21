import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/diagram/diagram-page/diagram-page.component').then(
        m => m.DiagramPageComponent,
      ),
  },
  { path: '**', redirectTo: '' },
];
