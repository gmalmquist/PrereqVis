# Garrett Malmquist 2013

import os, sys, io
import urllib
import re

def ReadClasses(path):
    if not isinstance(path, str):
        arr = []
        for p in path:
            arr += ReadClasses(p)
        return arr
    
    h = file(path, 'r')
    lines = h.readlines()
    h.close()

    classes = []
    
    for l in lines:
        if re.match('^[A-Z]+ \\d{4} - \\w+', l) != None:
            space = l.find(' ')
            subj = l[:space]
            l = l[space+1:]

            space = l.find(' ')
            num = l[:space]
            l = l[space+1:]

            space = l.find(' ')
            l = l[space+1:]

            name = l.strip()

            classes.append((subj, num, name))

    return classes

def ParsePrereqsOld(line):
    if len(line) == 0:
        return []
    
    reqs = []

    i = line.find('(')
    while i >= 0:
        reqs += ParsePrereqs(line[:i])
        line = line[i+1:]
        i = line.find(')')
        par = ParsePrereqs(line[:i])
        if len(par) > 0:
            reqs += [par]
        line = line[i+1:]
        i = line.find('(')

    for course in re.finditer('[A-Z]+ [0-9X]{4}', line):
        c = course.group(0)
        if len(c) > 0:
            reqs += [c]

    return reqs

def ParseTree(text):
    tokens = []
    par = 0

    word = ''
    
    for i in range(len(text)):
        c = text[i]
        if par > 0:
            if c == ')':
                par -= 1
            elif c == '(':
                par += 1
                
            if par == 0:
                tokens.append(ParseTree(word))
                word = ''
            else:
                word += c
        else:
            if c == '(':
                if len(word) > 0:
                    tokens.append(word)
                    word = ''
                par += 1
            else:
                if c == '|' or c == '&':
                    if len(word) > 0: tokens.append(word)
                    tokens.append(c)
                    word = ''
                else:
                    word += c

    if len(word) > 0:
        tokens.append(word)
                    
    return tokens
            
def Stackify(tree):
    text = ''

    for j in range(1, len(tree)):
        i = len(tree) - j
        # if find operator, stick it after operands
        # if t[-1] = op, t[0] = op, t[-1] = t[0]
        if tree[i-1] == '|' or tree[i-1] == '&':
            p = tree[i-1]
            tree[i-1] = tree[i]
            tree[i] = p
        
    for t in tree:
        if len(text) > 0: text += ' '
        if isinstance(t, str):
            text += t
        else:
            text += Stackify(t)

    return text

def ParsePrereqs(line):
    courses = []
    for match in re.finditer('[A-Z]+ [0-9X]{4,}', line):
        course = match.group(0)
        if len(course) > 0:
            if not course in courses:
                courses.append(course)

    line = line.upper()

    # Replace course names with unique, regex-friendly tokens
    i = 0
    for c in courses:
        line = re.sub(c, 'c'+str(i), line)
        i += 1
        
    line = re.sub(' AND ', '&', line)
    line = re.sub(' OR ', '|', line)
    # Get rid of numbers that aren't course listings
    line = re.sub('\\s\\d+', '', line)
    # Make sure there weren't more and's and or's than there
    # should be
    line = re.sub('[|]{2,}', '|', line)
    line = re.sub('[&]{2,}', '&', line)

    print 'O:', line

    line = re.sub('[^()&|0-9c]', '', line)

    # Because you can get invalid prereqs, like SAT scores
    # or AP credits, which the system doesn't support
    while line.endswith('&') or line.endswith('|'):
        line = line[:-1]
    while line.startswith('&') or line.startswith('|'):
        line = line[1:]

    # This is necessary to get rid of ambiguities that I
    # noticed in some PSYC course listings.
    # It turns a&b&c|d -> a&b&(c|d), rather than being
    # (a&b&c)|d, which the script was previously assuming.
    print 'A:', line
    line = re.sub(r'(c[0-9]+)\|(c[0-9]+)', r'(\1|\2)', line)
    print 'B:', line

    tree = ParseTree(line)
    stack = Stackify(tree)
    text = re.sub(' ', ',', stack)
    i = 0
    for c in courses:
        text = re.sub('c'+str(i), c, text)
        i += 1
    print text
    return text
    
    #print line
    #print ParseTree(line)
    #print Stackify(ParseTree(line))
    #return [line]
    

def FetchClassInfo(c):
    subj, num, name = c

    url = ''.join([
        'https://oscar.gatech.edu/pls/bprod/',
        'bwckctlg.p_disp_course_detail?cat_term_in=201305',
        '&subj_code_in=', str(subj),
        '&crse_numb_in=', str(num)
    ])
    print url

    keepTrying = True

    while keepTrying:
        try:
            handle = urllib.urlopen(url)
            lines = handle.readlines()
            handle.close()
            keepTrying = False
        except:
            print '\tConnection failed, retrying...'
            continue
        

    # Remove HTML junk that we don't care about
    newlines = []
    tag = False
    for i in range(len(lines)):
        l = lines[i].strip()

        while True:
            if not tag:
                car = l.find('<')
                if car < 0: # done
                    if len(l) > 0:
                        newlines.append(l)
                    break
                sta = car
            else:
                sta = 0
            car = l.find('>')
            if car < 0: # EOL and still unclosed
                tag = True
                if sta > 0:
                    newlines.append(l[:sta])
                break
            tag = False
            l = l[:sta] + l[car+1:]
    l = None # no.

    attrs = []
    preqs = ''

    preq = False
    for line in newlines:
        line = line.strip()
        if not preq:
            if line == 'Prerequisites:':
                preq = True
                continue
            if line.startswith('Course Attributes:'):
                attrs = line[len('Course Attributes:'):].strip().split(', ')
        else:
            if line == 'Return to Previous':
                preq = False
                continue
            #if line.find('(') < 0 and line.find(' or ') >= 0:
            #    line = '(' + line + ')'
            preqs += line + ' '

    preqs = ParsePrereqs(preqs)
    return attrs, preqs

class Course(object):
    def __init__(self, subj, number, name, attrs, preqs):
        self.subj = subj
        self.number = number
        self.name = name
        self.attrs = attrs
        self.preqs = preqs
        self.id = ' '.join((subj, number))

    def __str__(self):
        return self.id

    def serialize(self):
        s = str(self.id)
        s += '\t' + str(self.name)
        s += '\t' + ','.join(self.attrs)
        s += '\t'
        '''for p in self.preqs:
            s += ' '
            if isinstance(p, str):
                s += p
            else:
                s += ','.join(p)'''
        s += self.preqs
        return s

def DictToCourse(d):
    # subj, num, name, attrs, preqs
    return Course(d['subj'], d['number'], d['name'], d['attrs'], d['preqs'])


### TESTING
if False:
    attrs, preqs = FetchClassInfo(('MATH', '1501', 'Psych'))
    print attrs
    print preqs
    sys.exit(0)


def ScrapeCourses(inputFiles):
    for inputPath in inputFiles:
        print 'Reading', inputPath
        handle = file('courses.txt', 'w')
        courses = {}
        for cl in ReadClasses(inputPath):
            subj, number, name = cl
            attrs, preqs = FetchClassInfo(cl)
            
            course = {}
            course['subj'] = subj
            course['number'] = number
            course['name'] = name
            course['attrs'] = attrs
            course['preqs'] = preqs
            course['id'] = ' '.join((subj, number))
            
            courses[course['id']] = course
            
            print course

        handle.write(str(courses))

        handle.close()

        print 'Writing data.txt'
        handle = file('data_' + str(inputPath), 'w')
        handle.write('Course\tName\tAttributes\tPrerequisites\n')
        keys = []
        for k in courses:
            keys.append(k)
        keys.sort()
        for k in keys:
            handle.write(DictToCourse(courses[k]).serialize() + '\n')
        handle.close()

        print 'Done with', inputPath
    print 'Scraping complete.'

datasrc = ['cs.txt',   'ece.txt',  'psyc.txt',
              'me.txt',   'ae.txt',   'chbe.txt',
              'chem.txt', 'bmed.txt', 'phys.txt',
              'biol.txt', 'math.txt']
datasrc = ['mgt.txt', 'cx.txt']
datasrc = ['lcc.txt']
ScrapeCourses(datasrc)

