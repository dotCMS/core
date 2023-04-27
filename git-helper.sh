#!/bin/bash

ACTIVE_BRANCHES_FILE="$HOME/.active_branches"

function read_active_branches() {
    if [ -f "$ACTIVE_BRANCHES_FILE" ]; then
        active_branches=()
        while IFS= read -r line; do
            active_branches+=("$line")
        done < "$ACTIVE_BRANCHES_FILE"
    else
        active_branches=()
    fi
}

function write_active_branches() {
    printf "%s\n" "${active_branches[@]}" > "$ACTIVE_BRANCHES_FILE"
}

function switch_branch() {
    local current_branch="$(git branch --show-current)"
    local branch_name=$1
    local message=${2:-"WIP"}
    git stash save "$current_branch:$message" --include-untracked
    git submodule foreach --recursive "current_branch=\$(git branch --show-current); git stash save \"\${current_branch}:$message\" --include-untracked"

    local branch_stash=$(git stash list --grep "$branch_name:" | cut -d ":" -f 1)

    if git show-ref --verify --quiet "refs/heads/$branch_name"; then

        git checkout "$branch_name"
        if [ -n "$branch_stash" ]; then
            git stash pop "$branch_stash"
        fi
        git submodule foreach --recursive "branch_stash=\$(git stash list --grep \"$branch_name:\" | cut -d \":\" -f 1); if [ -n \"$branch_stash\" ]; then git stash apply \"$branch_stash\"; fi"
    else
        # create branch
        create_branch "$branch_name"
    fi

    # add branch to list of active branches
    read_active_branches
    if ! [[ "${active_branches[*]}" =~ (^|[[:space:]])"$branch_name"($|[[:space:]]) ]]; then
        active_branches+=("$branch_name")
        write_active_branches
    fi
}

function select_branch() {
    read_active_branches
    local branch_idx
    printf "Select a branch to switch to:\n"
    for i in "${!active_branches[@]}"; do
        printf "%s. %s\n" "$i" "${active_branches[$i]}"
    done
    read -p "Enter branch index: " branch_idx
    if [[ "$branch_idx" =~ ^[0-9]+$ ]] && (( branch_idx >= 0 )) && (( branch_idx < ${#active_branches[@]} )); then
        switch_branch "${active_branches[$branch_idx]}"
    else
        printf "Invalid branch index.\n"
    fi
}
# Function to create a branch in the parent and submodule repositories
create_branch() {
  local branch_name="$1"
  git checkout -b "$branch_name" origin/master
  git submodule foreach --recursive "git checkout -b \"$branch_name\" origin/master"
}

# Function to commit changes in the submodule and parent repositories with the same commit message
commit_changes() {
  local commit_msg="$1"
  git submodule foreach --recursive "git add .; git commit -m \"$commit_msg\";"
  git add .
  git commit -m "$commit_msg"
}

# Function to push changes in the submodule and parent repositories
push_changes() {
  local branch_name="$(git branch --show-current)"
  git push --set-upstream origin "$branch_name"
  git submodule foreach --recursive "git push --set-upstream origin \"$current_branch\";"
}


# Main function
main() {
  local action="$1"
  case "$action" in
    create_branch)
      create_branch "$2"
      ;;
    commit_changes)
      commit_changes "$2"
      ;;
    push_changes)
      push_changes
      ;;
    list_assigned_issues)
      list_assigned_issues "$2"
      ;;
    store_github_token)
      store_github_token "$2"
      ;;
    switch_branch)
          switch_branch "$2"
          ;;
    select_branch)
          select_branch
          ;;
    *)
      echo "Usage: $0 {create_branch|commit_changes|push_changes}"
      exit 1
  esac
}

main "$@"