#!/usr/bin/env python2.7
import argparse
import csv 
import re
import os
import sys
import codecs
import itertools
from test.test_math import acc_check

def toPercent(row, a, b):
    part = row[a]
    total = row[b]
    
    if part != None and total != None and part != 'null' and total != 'null':
        totalF = float(total);
        if totalF == 0:
            return 0.0
        return 100.0 * (float(part) / float(total)) 
    return None

def toInt(row, a):
    value = row[a]
    if value != None and value != 'null':
        return int(value)
    return None 

def timeInNanosToSeconds(row, a):
    value = row[a]
    if value != None and value != 'null':
        return float(value) / 1000000000.0
    return None 

def toFloat(row, a):
    value = row[a]
    if value != None and value != 'null':
        return float(value)
    return None 

# Mappings for TACAS Taipan Paper 
# mLatexSettingsMappings = {
# 'svcomp-Reach-32bit-Automizer_Default+AIv2_COMP_Simple.epf' : '\\setComp',
# 'svcomp-Reach-32bit-Automizer_Default+AIv2_COMP_Simple_total.epf' : '\\setCompT',
# 'svcomp-Reach-32bit-Automizer_Default+AIv2_INT.epf' : '\\setInt',
# 'svcomp-Reach-32bit-Automizer_Default+AIv2_INT_total.epf' : '\\setIntT',
# 'svcomp-Reach-32bit-Automizer_Default+AIv2_OCT.epf' : '\\setOct',
# 'svcomp-Reach-32bit-Automizer_Default+AIv2_OCT_total.epf' : '\\setOctT',
# 'svcomp-Reach-32bit-Automizer_Default.epf' : '\\setAuto',
# }

# Mappings for Interpol PLDI17 Paper 
mLatexSettingsMappings = {
'Z3-FP-UC-LV-Float-Kojak.epf'        : '\\zzztip',
'Mathsat-FP-UC-LV-Float-Kojak.epf'   : '\\mathsattip',
'Z3-FP-UC-Float-Kojak.epf'           : '\\spic',
'Z3-FP-LV-Float-Kojak.epf'           : '\\splv',
'Z3-FP-Float-Kojak.epf'              : '\\sponly',
'Z3-BP-UC-LV-Float-Kojak.epf'        : '\\wpiclv',
'Z3-BP-LV-Float-Kojak.epf'           : '\\wplv',
'Z3-BP-UC-Float-Kojak.epf'           : '\\wpic',
'Z3-BP-Float-Kojak.epf'              : '\\wponly',
 }

# Those are the dvips colors of xcolor 
# mLatexColors = ['Apricot', 'Aquamarine', 'Bittersweet', 'Black', 'Blue', 'BlueGreen', 'BlueViolet',
#                 'BrickRed', 'Brown', 'BurntOrange', 'CadetBlue', 'CarnationPink', 'Cerulean', 'CornflowerBlue',
#                 'Cyan', 'Dandelion', 'DarkOrchid', 'Emerald', 'ForestGreen', 'Fuchsia', 'Goldenrod', 'Gray',
#                 'Green', 'GreenYellow', 'JungleGreen', 'Lavender', 'LimeGreen', 'Magenta', 'Mahogany', 'Maroon',
#                 'Melon', 'MidnightBlue', 'Mulberry', 'NavyBlue', 'OliveGreen', 'Orange', 'OrangeRed', 'Orchid',
#                 'Peach', 'Periwinkle', 'PineGreen', 'Plum', 'ProcessBlue', 'Purple', 'RawSienna', 'Red',
#                 'RedOrange', 'RedViolet', 'Rhodamine', 'RoyalBlue', 'RoyalPurple', 'RubineRed', 'Salmon',
#                 'SeaGreen', 'Sepia', 'SkyBlue', 'SpringGreen', 'Tan', 'TealBlue', 'Thistle', 'Turquoise',
#                 'Violet', 'VioletRed', 'White', 'WildStrawberry', 'Yellow', 'YellowGreen', 'YellowOrange' ]

mLatexColors = ['s1', 's2', 's3', 's4', 's5', 's6', 's7', 's8', 's9', 'black', 'OliveGreen']

mLatexPlotMarks = ['star', 'triangle', 'diamond', 'x', '|', '10-pointed-star', 'pentagon', 'o']
mLatexPlotMarkRepeat = 10
mLatexPlotLines = ['solid', 'dotted', 'dashed' ]

mUltimateHeaders = []
mFriendlySettingNames = {}
mNecessaryHeaders = ['Settings', 'Toolchain', 'Result', 'File']


mRowFuns = [ ('Time' , lambda r : timeInNanosToSeconds(r, 'OverallTime'), 'semilogyaxis'),
             ('Iter' , lambda r : toInt(r, 'OverallIterations'), 'axis'),
             ('InterTime' , lambda r : timeInNanosToSeconds(r, 'TraceCheckerStatistics_InterpolantComputationTime'), 'semilogyaxis'),
             ('UnsatSize' , lambda r : toPercent(r, 'TraceCheckerStatistics_ConjunctsInUnsatCore', 'TraceCheckerStatistics_ConjunctsInSsa'), 'semilogyaxis'),
             ('QuantPreds' , lambda r : toPercent(r, 'TraceCheckerStatistics_QuantifiedInterpolants', 'TraceCheckerStatistics_ConstructedInterpolants'), 'axis'),
             ('PerfInter' , lambda r : toPercent(r, 'TraceCheckerStatistics_PerfectInterpolantSequences', 'TraceCheckerStatistics_InterpolantComputations'), 'axis'),
            ]
# # row funs for tacas taipan 
# mRowFuns = { 
#            'Time' : lambda r : timeInNanosToSeconds(r, 'Overall time'),
#            'Runtime' : lambda r : timeInNanosToSeconds(r, 'OverallTime'),
#            'TotalIterations' : lambda r : toInt(r, 'OverallIterations'),
#            'AIIterations' : lambda r : toInt(r, 'AbstIntIterations'),
#            'AI Refinements' : lambda r : toInt(r, 'AbstIntStrong'),
#            'Iter' : lambda r : toInt(r, 'Overall iterations'),
#            'InterpolantTime' : lambda r : timeInNanosToSeconds(r, 'TraceCheckerBenchmark_InterpolantComputationTime'),
#            'SizeReduction':lambda r : toPercent(r, 'TraceCheckerBenchmark_Conjuncts in UnsatCore', 'TraceCheckerBenchmark_Conjuncts in SSA'),
#            'QuantPreds':lambda r : toPercent(r, 'TraceCheckerBenchmark_QuantifiedInterpolants', 'TraceCheckerBenchmark_ConstructedInterpolants'),
#            }

def parseArgs():
    # parse command line arguments
    parser = argparse.ArgumentParser(description='Ultimate Latex table generator')
    parser.add_argument('input', type=str, nargs=1, help='A .csv file generated by an Ultimate test suite')
    parser.add_argument('-o', '--output', dest='output', type=str, nargs='?', help='Path to output directory. If not specified, use current working directory.')
    parser.add_argument('-n', '--table-name', dest='name', help='The name of the table we should produce')
    parser.add_argument('-d', '--with-document', dest='withDoc', action='store_true', help='Should we just print the table or also generate a surrounding document?')

    args = parser.parse_args()
    print 'Arguments:', args
    return args

def getSvcompSubFolder(input):
    return re.search('svcomp/(.*)/', input).group(1)

def getSuffix(prefix, input):
    return re.search('.*' + prefix + '(.*)', input).group(1)

def parseCsvFile(fname):
    csvfile = open(fname, 'rb')
    try:
        dialect = csv.Sniffer().sniff(csvfile.read(2048), delimiters=',')
    except csv.Error:
        print "Could not guess .csv dialect, assuming Ultimate defaults"
        csv.register_dialect('ultimate', delimiter=',')
        dialect = 'ultimate'
    csvfile.seek(0)
    return csv.DictReader(csvfile, dialect=dialect)

def mapCsv(reader, fun, *args):
    acc = None
    for row in reader:
        acc = fun(row, acc, *args)
    return acc

def reduceWithArgs(col, fun, init, *args):
    acc = init
    for elem in col:
        acc = fun(elem, acc, *args)
    return acc

def printFields(row, acc):
    for field in mUltimateHeaders:
        print row[field],
    print
    return

def getUniqueSet(fieldname, row, acc):
    if acc == None:
        acc = set()
    acc.add(row[fieldname])
    return acc

def getFolders(row, acc):
    if acc == None:
        acc = {}
    for field in mUltimateHeaders:
        input = row['File']
        key = getSvcompSubFolder(input)
        if(not key in acc):
            acc[key] = []
        acc[key].append(input)
    return acc

def getResultCountPerSetting(result, row, acc):
    if acc == None:
        acc = {}
    
    setting = row['Settings']
    resultCounter = 0
    if setting in acc:
       resultCounter = acc[setting]
    
    if row['Result'] in result:
        acc[setting] = resultCounter + 1 
        
    return acc

def getResultInputPerSetting(result, row, acc):
    if acc == None:
        acc = {}
    
    setting = row['Settings']
    resultInput = set()
    if not setting in acc:
       acc[setting] = resultInput
    else:
        resultInput = acc[setting]
    
    if row['Result'] in result:
        resultInput.add(row['File']) 
        
    return acc

def getExclusivePerSetting(rows, results):
    matchingInputs = mapCsv(rows, lambda x, y : getResultInputPerSetting(results, x, y))
    acc = {}
    for key, value in matchingInputs.iteritems():
        exclusive = value
        for okey, ovalue in matchingInputs.iteritems():
            if ovalue == value:
                continue
            exclusive = exclusive.difference(ovalue)
            if len(exclusive) == 0:
                break
        acc[key] = exclusive
    return acc

def getExclusiveCountPerSetting(rows, results):
    return mapValues(lambda x : len(x), getExclusivePerSetting(rows, results))

def getMixedInputs(rows, results):
    matchingInputs = mapCsv(rows, lambda x, y : getResultInputPerSetting(results, x, y))
    shared = set.intersection(*matchingInputs.values())
    exclusive = getExclusivePerSetting(rows, results).values()  
    pure = shared.union(*exclusive)  
    return set.union(*matchingInputs.values()).difference(pure)

def getResultPerPortfolioAny(rows, portfolio, results):
    successCounts = mapCsv(rows, lambda x, y : getResultInputPerSetting(results, x, y))
    goodResults = set()
    for key, value in successCounts.iteritems():
        if(key in portfolio):
            goodResults = goodResults.union(value)
        
    return goodResults

def getResultPerPortfolioAll(rows, results, uniqueFiles):
    rowsEveryoneCouldSolve = []
    goodResults = []
    for file in uniqueFiles:
        rowsPerFile = filter(lambda x : x['File'] == file, rows)
        if reduce(lambda acc, x : acc and x['Result'] in results, rowsPerFile, True):
            goodResults = goodResults + rowsPerFile
            # print "All of " + file + " have one of the results " + ' '.join(results)
    
    return goodResults        
    

def getCrashedInputs(rows, uniqueSettings):
    acc = {}
    max = 0
    for row in rows:
        file = row['File']
        if file in acc:
           settings = acc[file]
        else:
            settings = set()
            acc[file] = settings
        settings.add(row['Settings'])
        if max < len(settings):
            max = len(settings)
    
    acc = {k:v for k, v in acc.iteritems() if len(v) < max}
    acc = mapValues(lambda v : uniqueSettings.difference(v), acc)
    return acc

def addRowsForCrashedInputs(rows, crashedInputs, uniqueToolchain):
    if len(rows) == 0:
        return
    
    protoRow = rows[0]
    newrows = {}
    for key, value in crashedInputs.iteritems():
        for setting in value:
            newrow = newRow(protoRow)
            newrow['File'] = key
            newrow['Settings'] = setting
            newrow['Result'] = 'ERROR'
            newrow['Toolchain'] = uniqueToolchain
            rows = rows + [newrow] 
    
    return rows

def newRow(row):
    newrow = {}
    for key in row.iterkeys():
        newrow[key] = None
    return newrow

def getPlottable(rows, rowFun, settings):
    acc = {}
    for setting in settings:
        list = []
        acc[setting] = list
        for row in rows:
            if not row['Settings'] in setting:
                continue
            value = rowFun(row)
            if not value == None:
                list.append(value)
        list.sort()
    return acc


def mapKeys(fun, dicti):
    return dict(map(lambda (k, v): (fun(k), v), dicti.iteritems()))

def mapValues(fun, dicti):
    return dict(map(lambda (k, v): (k, fun(v)), dicti.iteritems()))

def min(val, acc):
    if val > acc:
        return acc
    else:
        return val

def max(val, acc):
    if val > acc:
        return acc
    else:
        return val

def getLatexPlotStyles():
    plotstylesLines = zip(mLatexColors, mLatexPlotLines)
    plotstylesMarks = zip(mLatexColors[len(mLatexPlotLines):], mLatexPlotMarks)
    acc = []
    for color, linestyle in plotstylesLines:
        acc.append('draw=' + color + ',' + linestyle)
    for color, markstyle in plotstylesMarks:
        acc.append('mark repeat={' + str(mLatexPlotMarkRepeat) + '},draw=' + color + ',solid,mark=' + markstyle)
    for color in mLatexColors[len(plotstylesLines) + len(plotstylesMarks):]:
        acc.append('draw=' + color + ',solid')
    return acc

def writeLatexPlotsPreamble(filename):
    f = codecs.open(filename, 'w', 'utf-8')
    f.write('%%%%%%%%%%% Commands for plots %%%%%%%%%%%\n')
    f.write('% argument #1: any options\n')
    f.write('\\newenvironment{customlegend}[1][]{%\n')
    f.write('    \\begingroup\n')
    f.write('    % inits/clears the lists (which might be populated from previous\n')
    f.write('    % axes):\n')
    f.write('    \\csname pgfplots@init@cleared@structures\\endcsname\n')
    f.write('    \\pgfplotsset{#1}%\n')
    f.write('}{%\n')
    f.write('    % draws the legend:\n')
    f.write('    \\csname pgfplots@createlegend\\endcsname\n')
    f.write('    \\endgroup\n')
    f.write('}%\n')
    f.write('\n')
    f.write('% makes \\addlegendimage available (typically only available within an\n')
    f.write('% axis environment):\n')
    f.write('\\def\\addlegendimage{\\csname pgfplots@addlegendimage\\endcsname}\n')
    f.write('\n')
    f.write('\\pgfplotsset{every axis/.append style={thick}}\n')
    f.write('\n')
    
    f.write('\\definecolor{s1}{RGB}{228,26,28}')
    f.write('\\definecolor{s2}{RGB}{55,126,184}')
    f.write('\\definecolor{s3}{RGB}{77,175,74}')
    f.write('\\definecolor{s4}{RGB}{152,78,163}')
    f.write('\\definecolor{s5}{RGB}{255,127,0}')
    f.write('\\definecolor{s6}{RGB}{255,255,51}')
    f.write('\\definecolor{s7}{RGB}{166,86,40}')
    f.write('\\definecolor{s8}{RGB}{247,129,191}')
    f.write('\\definecolor{s9}{RGB}{153,153,153}')
    
    f.write('\\pgfplotsset{\n')
    f.write('    mark repeat/.style={\n')
    f.write('        scatter,\n')
    f.write('        scatter src=x,\n')
    f.write('        scatter/@pre marker code/.code={\n')
    f.write('            \\pgfmathtruncatemacro\\usemark{\n')
    f.write('                or(mod(\\coordindex,#1)==0, (\\coordindex==(\\numcoords-1))\n')
    f.write('            }\n')
    f.write('            \\ifnum\\usemark=0\n')
    f.write('                \\pgfplotsset{mark=none}\n')
    f.write('            \\fi\n')
    f.write('        },\n')
    f.write('        scatter/@post marker code/.code={}\n')
    f.write('    }\n')
    f.write('}\n')
    
    
    f.write('\\pgfplotsset{cycle list={%\n')
    for style in getLatexPlotStyles():
        f.write('{' + style + '},\n')
    f.write('}}\n')

    f.write('%%%%%%%%%%%%% end commands for plots\n')
    f.close()
    return

 
def writeLatexPlotLegend(f, namesAndStyles):
    legendentriesstr = ''
    for name, (file, style) in namesAndStyles:
        legendentriesstr = legendentriesstr + name + ','
    
    f.write('    \\begin{tikzpicture}[scale=\\plotscale]\n')
    f.write('    \\begin{customlegend}[legend columns=' + str(len(namesAndStyles) / 2) + ',legend style={align=left,draw=none,column sep=2ex,thick},\n')
    f.write('                          legend entries={' + legendentriesstr + '}]\n')
    for name, (file, style) in namesAndStyles:
        f.write('        \\addlegendimage{' + style + '}\n')
    f.write('    \\end{customlegend}\n')
    f.write('    \\end{tikzpicture}\n')
    return

def writeLatexPlot(f, xlabel, ylabel, files, namesAndStylesDict, caption, axis):
    f.write('\\begin{tikzpicture}[scale=\\plotscale]\n')
    f.write('\\begin{' + axis + '}[%\n')
    f.write('log ticks with fixed point,%\n')
    f.write('xmin=0, ymin=0,%\n')
    f.write('xlabel={' + xlabel + '},%\n')
    f.write('ylabel={' + ylabel + '},grid=major,%\n')
    f.write('legend style={at={(0.025,0.975)},anchor=north west,legend cell align=left}%\n')
    f.write(']%\n')
    f.write('\\addlegendimage{empty legend}\\addlegendentry{' + caption + '}\n')
    for file, name in files:
        f.write('\\addplot[' + namesAndStylesDict[name][1] + '] table {fig/plots/' + file + '};\n')
    f.write('\\end{' + axis + '}\n')
    f.write('\\end{tikzpicture}\n')
    return

def createLatexPlots(successrows, uniqueSettings, tcName, outputDir, name):
    latexFigures = []
    for funName, fun, axis in mRowFuns:
        print 'Writing plot for ' + funName
        plottable = getPlottable(successrows, fun, map(lambda x : (x), uniqueSettings))
        plotfiles = []
        plotnames = []
        for setting, values in plottable.iteritems():
            friendlySetting = mFriendlySettingNames[setting]
            #friendlySetting = os.path.basename(setting)
            filename = tcName + '-' + funName + '-' + os.path.basename(setting) + '.plot'
            f = codecs.open(os.path.join(outputDir, filename), 'w', 'utf-8')
            i = 0
            for val in values:
                f.write(str(i) + ' ' + str(val) + '\n')
                i = i + 1
            f.close()
            if os.stat(f.name).st_size == 0:
                os.remove(f.name)
            else:
                plotfiles.append(filename)
                if friendlySetting in mLatexSettingsMappings:
                    plotnames.append(mLatexSettingsMappings[friendlySetting])
                else:
                    plotnames.append(friendlySetting)
        if name != '':
            funName = name + '-' + funName
        latexFigures.append((funName, zip(plotfiles, plotnames), axis))
    return latexFigures

def getNamesAndStyles(latexFigures):
    namesAndStylesDict = {}
    styles = iter(getLatexPlotStyles())    
    for key, val, val2 in latexFigures:
        for file, pname in val:
            if not pname in namesAndStylesDict:
                namesAndStylesDict[pname] = (file, next(styles))
    
    return sorted(namesAndStylesDict.items()), namesAndStylesDict

def writePlots(successrows, toolchain, uniqueSettings, outputDir, name):
    tcName = os.path.splitext(os.path.basename(toolchain))[0]
    writeLatexPlotsPreamble(os.path.join(outputDir, tcName + 'plots-pre.tex'))

    latexFigures = createLatexPlots(successrows, uniqueSettings, tcName, outputDir, name);
    namesAndStyles, namesAndStylesDict = getNamesAndStyles(latexFigures)
    
    plotsfile = os.path.join(outputDir, tcName + '-legend.tex')
    legendFile = codecs.open(plotsfile, 'w', 'utf-8')
    writeLatexPlotLegend(legendFile, namesAndStyles)
    legendFile.close()
    
    figCounter = 1
    figPerLine = 3    
    for funName, filesAndNames, axis in latexFigures:
        plotsfile = os.path.join(outputDir, tcName + '-' + funName + '-plots.tex')
        f = codecs.open(plotsfile, 'w', 'utf-8')    
        sortedByName = sorted(filesAndNames, key=lambda x : x[1])
        # f.write('\\resizebox*{0.45\\textwidth}{!}{%\n')
        writeLatexPlot(f, 'x', 'y', sortedByName, namesAndStylesDict, funName, axis)
        # f.write('}\n')
        f.close()        
    return

def getArgs():
    args = parseArgs()
    file = args.input[0]
    
    if not os.path.isfile(file):
        print file, 'does not exist'
        sys.exit(1)
        return
    
    output = args.output
    if output == None:
        output = os.getcwd()
    
    name = args.name
    if name == None:
        name = ''
        
    return file, output, name              

def isInt(s):
    try: 
        int(s)
        return True
    except ValueError:
        return False
    except TypeError:
        return False


def getStats(rows, title):
    sortedByIter = sorted(map(lambda row : int(row[title]), filter(lambda row: isInt(row[title]), rows)))
    lenSorted = len(sortedByIter)
    if lenSorted > 0:
        if len(rows) > lenSorted:
            print 'Lost', len(rows) - lenSorted, 'rows for stats because there was no value'
        avg = reduceWithArgs(sortedByIter, lambda iter, acc : iter + acc, 0) / lenSorted
        min = sortedByIter[0]
        max = sortedByIter[lenSorted - 1]
        if len(rows) % 2 == 0:
            a = lenSorted / 2;
            iterMed = (sortedByIter[lenSorted // 2] + sortedByIter[lenSorted // 2 + 1]) / 2.0
        else:
            iterMed = sortedByIter[lenSorted // 2 + 1]
    else:
        avg = 'N/A'
        min = 'N/A'
        max = 'N/A'
        iterMed = 'N/A'
    return avg, min, max, iterMed

def printStats(type, rowsEveryoneCouldSolve, setting, column):
    rowsEveryoneCouldSolve = filter(lambda x : x['Settings'] == setting, rowsEveryoneCouldSolve)
    iterAvg, iterMin, iterMax, iterMed = getStats(rowsEveryoneCouldSolve, column)
    print type, column, 'Avg:', iterAvg
    print type, column, 'Min:', iterMin
    print type, column, 'Max:', iterMax
    print type, column, 'Med:', iterMed

def checkCsv(rows):
    headers = rows[0];
    for header in mNecessaryHeaders:
        if not header in headers:
            print 'Necessary header ' + header + ' not present in input'
            print headers 
            sys.exit(1)
    mUltimateHeaders = sorted(list(headers.keys()))
    print 'Available Headers:'
    print mUltimateHeaders

def main():
    file, output, name = getArgs()
    
    successResults = ['SAFE', 'UNSAFE','CORRECT','INCORRECT']
    timeoutResults = ['TIMEOUT']
    failResults = ['FAIL']

    allRows = list(parseCsvFile(file))
    
    checkCsv(allRows);
    uniqueToolchains = mapCsv(allRows, lambda x, y : getUniqueSet('Toolchain', x, y))
    
    for toolchain in uniqueToolchains:
        print '### ' + toolchain + ' ###'
        rows = filter(lambda row: row['Toolchain'] == toolchain, allRows)
        uniqueSettings = mapCsv(rows, lambda x, y : getUniqueSet('Settings', x, y))
        uniqueFiles = mapCsv(rows, lambda x, y : getUniqueSet('File', x, y))
        
        crashed = getCrashedInputs(rows, uniqueSettings)
        rows = addRowsForCrashedInputs(rows, crashed, next(iter(uniqueToolchains)))
    
    
        commonSettingsPrefix = os.path.commonprefix(uniqueSettings)
        
        renameSettings = lambda x : mLatexSettingsMappings[x[len(commonSettingsPrefix):]] if x[len(commonSettingsPrefix):] in mLatexSettingsMappings else x[len(commonSettingsPrefix):]
        for setting in uniqueSettings:
            mFriendlySettingNames[setting] = renameSettings(setting)
        
        solversOnlySettings = filter(lambda x: not re.match('.*FP.*|.*BP.*', os.path.basename(x)), uniqueSettings)
        championsSettings = filter(lambda x: re.match('.*FP-UC-LV.*', os.path.basename(x)), uniqueSettings)
    
        
        # one line of unique settings: total success
        success = mapCsv(rows, lambda x, y : getResultCountPerSetting(successResults, x, y))
        timeout = mapCsv(rows, lambda x, y : getResultCountPerSetting(timeoutResults, x, y))
        fail = mapCsv(rows, lambda x, y : getResultCountPerSetting(failResults, x, y))
        exclusive = getExclusiveCountPerSetting(rows, successResults)
        
        allPortfolio = getResultPerPortfolioAny(rows, uniqueSettings, successResults)
        allTOPortfolio = getResultPerPortfolioAll(rows, timeoutResults, uniqueFiles)
        allFailPortfolio = getResultPerPortfolioAll(rows, failResults, uniqueFiles)
        otherPortfolio = getResultPerPortfolioAny(rows, solversOnlySettings, successResults)
        championsPortfolio = getResultPerPortfolioAny(rows, championsSettings, successResults)
    
        mixed = getMixedInputs(rows, successResults)
        
        remPathD = lambda x : mapKeys(lambda y : mFriendlySettingNames[y], x)
        remPathS = lambda x : map(lambda y : mFriendlySettingNames[y], x)
    
        print 'Settings:         ', remPathS(uniqueSettings)
        print 'Total inputs:     ', len(uniqueFiles)
        print 'Crashed inputs #: ', len(crashed)
        print 'Crashed inputs:   ', crashed
        print 'Success:          ', remPathD(success)
        print 'Timeout:          ', remPathD(timeout)
        print 'Error:            ', remPathD(fail)
        print 'Exclusive success:', remPathD(exclusive)
        print 'All Portfolio:    ', len(allPortfolio)
        print 'All Timeout:      ', len(allTOPortfolio) / len(uniqueSettings)
        print 'All Error:        ', len(allFailPortfolio) / len(uniqueSettings)
        print '# Craig Portfolio:  ', len(otherPortfolio)
        print 'Craig Portfolio:  ', remPathS(solversOnlySettings)
        print '# Craig+IT-SP Portfolio: ', len(championsPortfolio)
        print 'Craig+IT-SP Portfolio: ', remPathS(championsSettings)
        
        # print 'Mixed:            ', mixed
        print 'Mixed Count:      ', len(mixed)
        
        rowsEveryoneCouldSolve = getResultPerPortfolioAll(rows, successResults, uniqueFiles)
        
        successrows = filter(lambda x : x['Result'] in successResults , rows)
        ecs = [i for i in uniqueSettings ]
        
        print 'Everyone', remPathS(ecs)
        print 'Everyone could solve (ECS):', len(rowsEveryoneCouldSolve) / len(uniqueSettings)
        
        # Use this if you want to print specific settings for the ECS set
        for s in ecs:
            print '## Setting', mFriendlySettingNames[s], '##'
            # printStats('ECS', rowsEveryoneCouldSolve, s, 'AbstIntIterations')
            # printStats('ECS', rowsEveryoneCouldSolve, s, 'AbstIntStrong')
            # printStats('ALL', successrows, s, 'AbstIntIterations')
            # printStats('ALL', successrows, s, 'AbstIntStrong')
            # printStats(rowsEveryoneCouldSolve, s, 'Overall time')
        print 
        
        # # gnuplot and stuff 
        writePlots(successrows, toolchain, uniqueSettings, output, name)
    
    return

if __name__ == "__main__":
    main()
