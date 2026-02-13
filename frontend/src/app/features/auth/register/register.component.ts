import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="card">
        <h2>Register</h2>
        <p>Register component placeholder - will be implemented in phase 13</p>
      </div>
    </div>
  `,
})
export class RegisterComponent {}
