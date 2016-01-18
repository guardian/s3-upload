import yaml
import json
from . import *


def convert():
    input, output = get_arguments()

    with open(input, 'r') as f:
        input_yaml = yaml.load(f)

    with open('{}.json'.format(output), 'w') as f:
        json.dump(input_yaml, f, indent=2)

if __name__ == '__main__':
    convert()
