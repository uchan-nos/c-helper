import os
import re

if __name__ == '__main__':
	test_input_dir = 'input'
	test_output_dir = 'output'
	test_result_dir = 'result'
	testcase_name = re.compile(r'(.+).in')
	input_files = os.listdir(test_input_dir)
	output_files = os.listdir(test_output_dir)
	for f in input_files:
		m = testcase_name.match(f)
		if m:
			name = m.group(1)
			if name + '.out' not in output_files:
				print('cannot find output file ' name + '.out')
			else:


