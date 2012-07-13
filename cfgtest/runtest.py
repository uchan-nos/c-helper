import os
import re
import subprocess

if __name__ == '__main__':
	test_input_dir = 'input'
	test_output_dir = 'output'
	test_result_dir = 'result'
	testcase_name = re.compile(r'(.+).c')
	input_files = os.listdir(test_input_dir)
	output_files = os.listdir(test_output_dir)
	test_passed = 0
	numtest = 0
	for f in input_files:
		m = testcase_name.match(f)
		if m and len(m.group(0)) == len(f):
			numtest += 1
			name = m.group(1)
			print('------------')
			print('running test "' + name + '"')
			print('------------')
			if name + '.dot' not in output_files:
				print('cannot find output file ' + name + '.dot')
			else:
				cmd = 'CLASSPATH=../bin:`python ./searchplugins.py` '+ \
						'java com.github.uchan_nos.c_helper.Launcher ' + \
						'input/' + name + '.c' + \
						' | diff -u output/' + name + '.dot -'
				p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
				(stdoutdata, stderrdata) = p.communicate()
				if stdoutdata != None and len(stdoutdata) <= 1:
					print('PASS')
					test_passed += 1
				elif stdoutdata != None and len(stdoutdata) > 1:
					print stdoutdata,
				else:
					print('stdout is null')
	print('-------------------------')
	print('Result: passed = %d / %d' % (test_passed, numtest))


