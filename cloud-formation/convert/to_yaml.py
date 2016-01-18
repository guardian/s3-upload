import yaml
import json
from . import *


def convert():
    input, output = get_arguments()

    with open(input, 'r') as f:
        input_json = json.load(f)

    with open('{}.yml'.format(output), 'w') as f:
        yaml.safe_dump(input_json, f, default_flow_style=False, canonical=False)


if __name__ == '__main__':
    convert()
