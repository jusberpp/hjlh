#!/bin/bash

set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Error: 请提供项目名称作为参数，例如: ./tag.sh studentAuth 或者 ./tag.sh gaokaoLive"
    exit 1
fi

project_name="$1"

if [[ ! "$project_name" =~ ^[a-z][A-Za-z0-9]*$ ]]; then
    echo "Error: '$project_name' 不合法，请使用驼峰项目名，例如 studentAuth、gaokaoLive"
    exit 1
fi

if [[ ! -f "src/pages/${project_name}/entry.ts" ]]; then
    echo "Error: 找不到 H5 项目入口 src/pages/${project_name}/entry.ts"
    exit 1
fi

# tag 格式模板：H5project#projectName
service_tag="H5project#${project_name}"

# 支持打 tag 的环境
SUPPORTED_ENVS="test uat prod"

function tag() {
    # 检查当前分支是否允许打 tag
    current_branch=$(git symbolic-ref --short -q HEAD)
    case "$current_branch" in
        test|uat)
            prefix="$current_branch"
            ;;
        main)
            prefix="prod"
            ;;
        *)
            echo "Error: 当前分支 '$current_branch' 不允许打 tag，仅支持以下环境: $SUPPORTED_ENVS"
            exit 1
            ;;
    esac

    git push
    git pull --tags

    local today=$(date +'%Y-%m-%d')
    local tag_prefix="${prefix}-${service_tag}-${today}"
    local latest_version=$(git tag -l "${tag_prefix}-*" |
        while read -r existing_tag; do
            local version="${existing_tag##*-}"
            if [[ "$version" =~ ^[0-9]+$ ]]; then
                echo "$version"
            fi
        done |
        sort -n |
        tail -n 1)

    local next_version=0
    if [[ -n "$latest_version" ]]; then
        next_version=$((10#$latest_version + 1))
    fi

    local new_tag=$(printf '%s-%02d' "$tag_prefix" "$next_version")
    echo "Creating tag: ${new_tag}"
    git tag "${new_tag}"
    git push origin "${new_tag}"
}
tag;
