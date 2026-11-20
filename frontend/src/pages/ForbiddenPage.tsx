
export default function ForbiddenPage() {
  return (
    <div className="forbidden-page">
      <h1>403 - Access Denied</h1>
      <p>
        You do not have permission to access this page.
      </p>
      <a href="/dashboard">Go to Dashboard</a>
    </div>
  );
}
