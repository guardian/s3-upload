import os
import sys


def _get_input_file():
    if len(sys.argv) == 1:
        converter = os.path.split(sys.argv[0])[1].split('.')[0]
        print('No input file. Usage: python -m convert.{} /path/to/file'.format(converter))
    else:
        return sys.argv[1]


def _get_output_file(input_file):
    filename, _ = os.path.splitext(os.path.basename(input_file))
    return filename


def get_arguments():
    input = _get_input_file()
    output = _get_output_file(input)

    return input, output
