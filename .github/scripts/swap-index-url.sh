#!/bin/bash

set -e

# Check for proper number of command line args.
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 path_to_json.xz new_url"
    exit 1
fi

# Assign command line args to variables
input_file="$1"
new_url="$2"
temp_json=$(mktemp -t modified-json-XXXXX)

# Decompress the .xz file into a temporary JSON file
xz -d -k -c "${input_file}" > "${temp_json}"

# Check if jq is installed
if ! command -v jq &> /dev/null
then
    echo "jq could not be found, please install jq to use this script."
    exit 1
fi

# Update the URL field using jq
jq --arg new_url "$new_url" '.entries[0].url = $new_url' "${temp_json}" > "${temp_json}.tmp" && mv "${temp_json}.tmp" "${temp_json}"

# Compress the modified JSON file back into the .xz format
xz -z -c "${temp_json}" > "${input_file}"

echo "The url field has been updated to $new_url and the file ${input_file} is modified"