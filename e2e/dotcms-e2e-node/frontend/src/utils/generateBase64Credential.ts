export function generateBase64Credentials(username: string, password: string) {
  const credentialsBase64 = btoa(`${username}:${password}`);
  return `Basic ${credentialsBase64}`;
}
