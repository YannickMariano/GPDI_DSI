import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from '@layout/sidebar/sidebar';
import { Header } from '@layout/header/header';
import { ToastTray } from '@shared/components/toast/toast-tray';
import { AssistantWidget } from '@layout/assistant-widget/assistant-widget';

@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, Sidebar, Header, ToastTray, AssistantWidget],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  protected readonly mobileNavOpen = signal(false);
}
