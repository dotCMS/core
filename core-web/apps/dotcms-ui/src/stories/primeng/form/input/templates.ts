export const InputTextTemplate = `<div class="flex flex-column gap-3 mb-2">
<div class="flex flex-column gap-2">
    <label htmlFor="username">Username</label>
    <input id="username" pInputText aria-describedby="username-help" placeholder="Placeholder" autocomplete="off" />
    <small id="username-help">Enter your username to reset your password.</small>
</div>
<div class="flex flex-column gap-2">
    <label htmlFor="username-error">Username</label>
    <input
        class="ng-invalid ng-dirty"
        id="username-error"
        pInputText
        aria-describedby="username-help-error"
        placeholder="Placeholder"
        autocomplete="off"
    />
    <small id="username-help-error">Please enter a valid username</small>
</div>
<div class="flex flex-column gap-2">
    <label htmlFor="username-disabled">Disabled</label>
    <input
        id="username-disabled"
        pInputText
        aria-describedby="username-help-disabled"
        disabled
        placeholder="Disabled"
        autocomplete="off"
    />
</div>
</div>
<h4>Small</h4>
<div class="flex flex-column gap-3">
<div class="flex flex-column gap-2">
    <label htmlFor="username">Username</label>
    <input class="p-inputtext-sm" id="username" pInputText aria-describedby="username-help" placeholder="Placeholder" autocomplete="off" />
    <small id="username-help">Enter your username to reset your password.</small>
</div>
<div class="flex flex-column gap-2">
    <label htmlFor="username-error">Username</label>
    <input
        class="ng-invalid ng-dirty p-inputtext-sm"
        id="username-error"
        pInputText
        aria-describedby="username-help-error"
        placeholder="Placeholder"
        autocomplete="off"
    />
    <small id="username-help-error">Please enter a valid username</small>
</div>
<div class="flex flex-column gap-2">
    <label htmlFor="username-disabled">Disabled</label>
    <input
        class="p-inputtext-sm"
        id="username-disabled"
        pInputText
        aria-describedby="username-help-disabled"
        disabled
        placeholder="Disabled"
        autocomplete="off"
    />
</div>
</div>
`;

export const InputTextAreaTemplate = `
  <textarea pInputTextarea [rows]="5" [cols]="30" placeholder="Some placeholder"></textarea>
`;

export const InputTextAreaTemplateAutoRezise = `
  <textarea [rows]="5" [cols]="30" pInputTextarea autoResize="autoResize"></textarea>
`;

export const InputSwitchTemplate = `<p-inputSwitch [(ngModel)]="checked"></p-inputSwitch>`;

export const InputNumberTemplate = `
  <p-inputNumber [(ngModel)]="val" mode="decimal"></p-inputNumber>
`;

export const InputMaskTemplate = `<p-inputMask [(ngModel)]="val" mask="99-9999" placeholder="99-9999"></p-inputMask>`;
