import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>Login</h2>
        <p>Login component placeholder - will be implemented in phase 13</p>
      </div>
    </div>
  `,
})
export class LoginComponent {}
