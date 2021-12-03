# How to handle the -Dexec.args and -Dexec.arguments properties to maven's exec:java goal

## Try -Dexec.arguments

```
mvn exec:java -Dexec.mainClass="com.example.App" -Dexec.arguments='O'\''Reilly,The answer is "yes","red, white, and blue"'
```

*Result:*
```
5 args: <O'Reilly> <The answer is "yes"> <"red> < white> < and blue">
```

- We see it uses comma separated values
- But, unfortunately, it is not compliant with https://en.wikipedia.org/wiki/Comma-separated_values
- It is, therefore, impossible to specify an argument containing a comma

## Try -Dexec.args

```
mvn exec:java -Dexec.mainClass="com.example.App" -Dexec.args='"O'\''Reilly" '\''The answer is "yes"'\'' "red, white, and blue"'
```

*Result:*
```
3 args: <O'Reilly> <The answer is "yes"> <red, white, and blue>
```

- We see it uses white space separated values
- Rudimentary quoting is supported using either single or double quotes
- It is possible to embed a single quote within a double-quoted string argument
- It is possible to embed a quotation mark with a single-quoted string argument
- It is impossible, however, to have an argument that contains both a quotation mark and an apostrophe

## Helper function to prepare bash arguments to be passsed to 

Here's a bash function to assist with all the escaping that is involved.

```bash
prepare_args() {
  local OPTIND opt
  local debug= # Set to any non-zero width string to enable debug output for this function
  while getopts ':d' opt; do
    case $opt in
      d)
        debug=1
        ;;
      \?)
        echo "error: $FUNCNAME: -$OPTARG: invalid option" >&2
        return 1
        ;;
      :)
        echo "error: $FUNCNAME: -$OPTARG: this option requires an argument" >&2
        return 1
        ;;
      *)
        echo "error: $FUNCNAME: An unexpected execution path occurred" >&2
        return 1
        ;;
    esac
  done
  shift $((OPTIND - 1))
  local args=()
  local both=  # This will get set to '1' if both an apostrophe and a quotation mark are detetected in an argument
  [[ -n $debug ]] && { echo; echo; } >&2
  for arg in "$@"; do
    case $arg in
      *\"*)
        case $arg in *\'*)
          [[ -n $debug ]] && printf '%20s: %s\n' "$arg"  "Both were detected" >&2
          echo >&2
          echo "prepare_args: error: An argument had both an apostrophe and a quotation mark" >&2
          echo "- Maven's exec.args property cannot handle such a case" >&2
          echo "- The argument was '$arg'" >&2
          return 1
        esac
        [[ -n $debug ]] && printf '%20s: %s' "$arg" "Embedded quotation mark detected" >&2
        args+=($(printf "'%s' " "${arg//\'/\\\'}"))
        ;;
      *\'*)
        [[ -n $debug ]] && printf '%20s: %s' "$arg"  "Embedded apostrophe detected" >&2
        args+=($(printf '"%s" ' "${arg//\"/\\\"}"))
        ;;
      *)
        [[ -n $debug ]] && printf '%20s: %s' "$arg"  "No worries " >&2
        args+=($(printf '"%s" ' "${arg//\"/\\\"}"))
        ;;
    esac
    [[ -n $debug ]] && echo >&2
  done
  [[ -n $debug ]] && echo
  echo "${args[@]}"
}
```

### *Test 1:*

```bash
prepare_args "O'Reilly" 'The answer is "Yes"' "red, white, and blue"
```

*Result:*

```none
"O'Reilly" 'The answer is "Yes"' "red, white, and blue"
```

### *Test 2: show debug output using the -d option:*

```bash
prepare_args -d "O'Reilly" 'The answer is "Yes"' "red, white, and blue" >/dev/null
```

*Result:*

```none
            O'Reilly: Embedded apostrophe detected
 The answer is "Yes": Embedded quotation mark detected
red, white, and blue: No worries
```


### *Example usage:*

```bash
exec_using_maven () {
  local exec_args
  exec_args=$(prepare_args -- "$@")
  if [[ $? != 0 ]]; then
    echo "exec_using_maven: Something went wrong while preparing the arguments">&2
    return 1
  fi
  mvn exec:java -Dexec.mainClass="com.example.App" -Dexec.args="$exec_args"
}

exec_using_maven "O'Reilly" 'The answer is "Yes"' "red, white, and blue"
```

*Result:*

```none
3 args: <O'Reilly> <The answer is "Yes"> <red, white, and blue>
```

✔️

Just don't try to pass an argument that contains both an apostrophe and a quotation mark.

