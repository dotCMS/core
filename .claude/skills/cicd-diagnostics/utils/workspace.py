#!/usr/bin/env python3
"""Diagnostic Workspace Management Utilities.

Handles creation, caching, and organization of diagnostic artifacts.
"""

import os
import subprocess
import stat
from pathlib import Path
from typing import Optional


def get_repo_root() -> Path:
    """Get repository root (works from any subdirectory)."""
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True,
            text=True,
            check=True
        )
        return Path(result.stdout.strip())
    except (subprocess.CalledProcessError, FileNotFoundError):
        return Path(".").resolve()


def create_diagnostic_workspace(run_id: str) -> Path:
    """Create diagnostic workspace (no timestamp - reusable by run ID).
    
    Args:
        run_id: GitHub Actions run ID
        
    Returns:
        Path to diagnostic directory
    """
    repo_root = get_repo_root()
    diagnostic_dir = repo_root / ".claude" / "diagnostics" / f"run-{run_id}"
    diagnostic_dir.mkdir(parents=True, exist_ok=True)
    return diagnostic_dir


def find_existing_diagnostic(run_id: str) -> Optional[Path]:
    """Find existing diagnostic workspace for a run ID.
    
    Args:
        run_id: GitHub Actions run ID
        
    Returns:
        Path to existing directory or None
    """
    repo_root = get_repo_root()
    diagnostic_dir = repo_root / ".claude" / "diagnostics" / f"run-{run_id}"
    
    if diagnostic_dir.exists() and diagnostic_dir.is_dir():
        return diagnostic_dir
    return None


def get_diagnostic_workspace(run_id: str, force_clean: bool = False) -> Path:
    """Get or create diagnostic workspace (with caching).
    
    Args:
        run_id: GitHub Actions run ID
        force_clean: If True, remove existing workspace and start fresh
        
    Returns:
        Path to diagnostic directory (existing or new)
    """
    repo_root = get_repo_root()
    diagnostic_dir = repo_root / ".claude" / "diagnostics" / f"run-{run_id}"
    
    # Clean existing workspace if requested
    if force_clean and diagnostic_dir.exists():
        print(f"🗑️  Cleaning existing workspace: {diagnostic_dir}", file=os.sys.stderr)
        import shutil
        shutil.rmtree(diagnostic_dir)
    
    if diagnostic_dir.exists():
        print(f"✓ Reusing existing diagnostic workspace: {diagnostic_dir}", file=os.sys.stderr)
        print("  (Cached logs and metadata will be reused)", file=os.sys.stderr)
        return diagnostic_dir
    else:
        diagnostic_dir.mkdir(parents=True, exist_ok=True)
        print(f"✓ Created new diagnostic workspace: {diagnostic_dir}", file=os.sys.stderr)
        return diagnostic_dir


def save_artifact(diagnostic_dir: Path, filename: str, content: str) -> None:
    """Save artifact to diagnostic workspace.
    
    Args:
        diagnostic_dir: Diagnostic workspace directory
        filename: Name of the file to save
        content: Content to write
    """
    artifact_path = diagnostic_dir / filename
    artifact_path.write_text(content, encoding='utf-8')


def artifact_exists(diagnostic_dir: Path, filename: str) -> bool:
    """Check if artifact exists in workspace.
    
    Args:
        diagnostic_dir: Diagnostic workspace directory
        filename: Name of the file to check
        
    Returns:
        True if exists and non-empty, False otherwise
    """
    artifact_path = diagnostic_dir / filename
    return artifact_path.exists() and artifact_path.stat().st_size > 0


def get_or_fetch_artifact(diagnostic_dir: Path, filename: str, fetch_command: list) -> Path:
    """Get cached artifact or fetch new.
    
    Args:
        diagnostic_dir: Diagnostic workspace directory
        filename: Name of the artifact file
        fetch_command: Command to run if artifact doesn't exist (list of args)
        
    Returns:
        Path to artifact file
    """
    artifact_path = diagnostic_dir / filename
    
    if artifact_exists(diagnostic_dir, filename):
        print(f"✓ Using cached artifact: {filename}", file=os.sys.stderr)
        return artifact_path
    else:
        print(f"Fetching {filename}...", file=os.sys.stderr)
        result = subprocess.run(
            fetch_command,
            capture_output=True,
            text=True,
            check=True
        )
        artifact_path.write_text(result.stdout, encoding='utf-8')
        print(f"✓ Saved to: {artifact_path}", file=os.sys.stderr)
        return artifact_path


def ensure_gitignore_diagnostics() -> None:
    """Ensure .gitignore includes diagnostic directories."""
    repo_root = get_repo_root()
    gitignore_path = repo_root / ".gitignore"
    
    gitignore_content = ""
    if gitignore_path.exists():
        gitignore_content = gitignore_path.read_text(encoding='utf-8')
    
    if ".claude/diagnostics/" not in gitignore_content:
        gitignore_content += "\n# Claude Code diagnostic outputs\n.claude/diagnostics/\n"
        gitignore_path.write_text(gitignore_content, encoding='utf-8')
        print("✓ Added .claude/diagnostics/ to .gitignore", file=os.sys.stderr)


def list_diagnostic_workspaces() -> list[Path]:
    """List all diagnostic workspaces.
    
    Returns:
        List of workspace paths, sorted by name (most recent first)
    """
    repo_root = get_repo_root()
    diagnostics_dir = repo_root / ".claude" / "diagnostics"
    
    if not diagnostics_dir.exists():
        return []
    
    workspaces = [
        p for p in diagnostics_dir.iterdir()
        if p.is_dir() and p.name.startswith("run-")
    ]
    return sorted(workspaces, reverse=True)


def get_workspace_age(diagnostic_dir: Path) -> int:
    """Get workspace age in hours.
    
    Args:
        diagnostic_dir: Diagnostic workspace directory
        
    Returns:
        Age in hours, or -1 if directory doesn't exist
    """
    if not diagnostic_dir.exists():
        return -1
    
    dir_timestamp = diagnostic_dir.stat().st_mtime
    current_timestamp = os.path.getmtime(diagnostic_dir)
    age_seconds = current_timestamp - dir_timestamp
    age_hours = int(age_seconds / 3600)
    
    return age_hours


def clean_old_diagnostics(max_age_hours: int = 168, max_count: int = 50) -> int:
    """Clean old diagnostic workspaces.
    
    Args:
        max_age_hours: Maximum age in hours (default: 168 = 7 days)
        max_count: Maximum number to keep (default: 50)
        
    Returns:
        Number of workspaces removed
    """
    print(f"Cleaning diagnostic workspaces older than {max_age_hours} hours...", file=os.sys.stderr)
    
    workspaces = list_diagnostic_workspaces()
    removed = 0
    
    for i, workspace in enumerate(workspaces, 1):
        age = get_workspace_age(workspace)
        
        if age >= max_age_hours or i > max_count:
            print(f"  Removing: {workspace} (age: {age}h)", file=os.sys.stderr)
            import shutil
            shutil.rmtree(workspace)
            removed += 1
    
    print(f"✓ Cleaned {removed} old diagnostic workspace(s)", file=os.sys.stderr)
    return removed


def get_workspace_summary(diagnostic_dir: Path) -> str:
    """Get workspace summary.
    
    Args:
        diagnostic_dir: Diagnostic workspace directory
        
    Returns:
        Summary string
    """
    if not diagnostic_dir.exists():
        return f"Workspace not found: {diagnostic_dir}"
    
    import shutil
    age = get_workspace_age(diagnostic_dir)
    size = shutil.disk_usage(diagnostic_dir).used
    
    lines = [
        "=== Diagnostic Workspace Summary ===",
        f"Path: {diagnostic_dir}",
        f"Age: {age} hours",
        f"Size: {size} bytes",
        "Files:"
    ]
    
    for file_path in sorted(diagnostic_dir.iterdir()):
        if file_path.is_file():
            size_str = f"{file_path.stat().st_size:,} bytes"
            lines.append(f"  {file_path.name:<40} {size_str:>10}")
    
    return "\n".join(lines)


def init_diagnostic_structure(diagnostic_dir: Path) -> None:
    """Create standard diagnostic file structure.
    
    Args:
        diagnostic_dir: Diagnostic workspace directory
    """
    diagnostic_dir.mkdir(parents=True, exist_ok=True)
    (diagnostic_dir / "error-summary.txt").touch()
    (diagnostic_dir / "analysis-notes.txt").touch()
    
    print(f"✓ Initialized diagnostic structure in {diagnostic_dir}", file=os.sys.stderr)


