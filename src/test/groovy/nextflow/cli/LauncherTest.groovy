/*
 * Copyright (c) 2013-2018, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2018, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

package nextflow.cli

import java.nio.file.Files

import picocli.CommandLine
import spock.lang.Specification
import test.OutputCapture
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class LauncherTest extends Specification {

    @org.junit.Rule
    OutputCapture capture = new OutputCapture()

    def 'should return `version` option' () {

        when:
        def launcher = new Launcher().parseMainArgs('-v')
        then:
        assert launcher.options.version

        when:
        launcher = new Launcher().parseMainArgs('--version')
        then:
        assert launcher.options.version
        assert launcher.fullVersion
    }

    def 'should return `help` command' () {

        when:
        def launcher = new Launcher().parseMainArgs('-h')

        then:
        assert launcher.options.help

        when:
        launcher = new Launcher().parseMainArgs('help')
        then:
        launcher.command instanceof CmdHelp
        launcher.command.args == null

        when:
        launcher = new Launcher().parseMainArgs('help','xxx')
        then:
        launcher.parsedCommand.command instanceof CmdHelp
        launcher.parsedCommand.command.args == ['xxx']

    }

    def 'should return `info` command'() {

        when:
        def launcher = new Launcher().parseMainArgs('info')
        then:
        launcher.parsedCommand.command instanceof CmdInfo
        launcher.parsedCommand.command.projectName == null

        when:
        launcher = new Launcher().parseMainArgs('info','xxx')
        then:
        launcher.parsedCommand.command instanceof CmdInfo
        launcher.parsedCommand.command.projectName == 'xxx'

    }

    def 'should return `pull` command'() {

        when:
        def launcher = new Launcher().parseMainArgs('pull','alpha')
        then:
        launcher.parsedCommand.command instanceof CmdPull
        launcher.parsedCommand.command.args == ['alpha']

        when:
        launcher = new Launcher().parseMainArgs('pull', '--hub', 'bitbucket', '--user','xx:11', 'xxx')
        then:
        launcher.parsedCommand.command instanceof CmdPull
        launcher.parsedCommand.command.args == ['xxx']
        launcher.parsedCommand.command.hubProvider == 'bitbucket'
        launcher.parsedCommand.command.hubUser == 'xx'
        launcher.parsedCommand.command.hubPassword == '11'

    }

    def 'should return `clone` command'() {
        when:
        def launcher = new Launcher().parseMainArgs('clone', '--hub', 'bitbucket', '--user', 'xx:yy', 'xxx')
        then:
        launcher.parsedCommand.command instanceof CmdClone
        launcher.parsedCommand.command.args == ['xxx']
        launcher.parsedCommand.command.hubProvider == 'bitbucket'
        launcher.parsedCommand.command.hubUser == 'xx'
        launcher.parsedCommand.command.hubPassword == 'yy'
    }


    def 'should return `run` command'() {
        given:
        CmdRun cmd

        when:
        cmd = new Launcher() .parseMainArgs('run', 'foo') .command
        then:
        cmd.workflow == 'foo'

        when:
        cmd = new Launcher() .parseMainArgs('run', 'foo', '--xx', 'yy') .command
        then:
        cmd.workflow == 'foo'
        cmd.args == ['--xx']

        when:
        cmd = new Launcher() .parseMainArgs('run', '--hub', 'bitbucket', '--user','xx:yy', 'xxx') .command
        then:
        cmd.workflow == 'xxx'
        cmd.hubProvider == 'bitbucket'
        cmd.hubUser == 'xx'
        cmd.hubPassword == 'yy'

//        when:
//        cmd = new Launcher().parseMainArgs('run', 'script.nf', '--alpha', '0', '--omega', '9') .command
//        then:
//        cmd.args == ['--alpha','0','--omega','9']

    }


    def 'should normalise command line options' () {

        given:
        def script = Files.createTempFile('file',null)
        def launcher = [:] as Launcher

        expect:
        launcher.normalizeArgs('a','-bb','-ccc','dddd') == ['a','-bb','-ccc','dddd']
        launcher.normalizeArgs('a','-bb','-ccc','--resume', 'last') == ['a','-bb','-ccc','--resume','last']
        launcher.normalizeArgs('a','-bb','-ccc','--resume') == ['a','-bb','-ccc','--resume','last']
        launcher.normalizeArgs('a','-bb','-ccc','--resume','1d2c942a-345d-420b-b7c7-18d90afc6c33', 'zzz') == ['a','-bb','-ccc','--resume','1d2c942a-345d-420b-b7c7-18d90afc6c33', 'zzz']

        launcher.normalizeArgs('x','--test') == ['x','--test','%all']
        launcher.normalizeArgs('x','--test','alpha') == ['x','--test','alpha']
        launcher.normalizeArgs('x','--test','--other') == ['x','--test','%all','--other']

//        launcher.normalizeArgs('--alpha=1') == ['--alpha=1']
//        launcher.normalizeArgs('--alpha','1') == ['--alpha=1']
//        launcher.normalizeArgs('run','--x') == ['run', '--x=true']
//        launcher.normalizeArgs('run','--x','--y') == ['run', '--x=true', '--y=true']
//        launcher.normalizeArgs('run','--x','--y', '-1', '--z') == ['run', '--x=true', '--y=-1', '--z=true']

        launcher.normalizeArgs('-x', '1', 'script.nf', '--long', 'v1', '--more', 'v2', '--flag') == ['-x','1','script.nf','--long=v1','--more=v2','--flag=true']

        launcher.normalizeArgs('-x', '1', '-process.alpha','2', '3') == ['-x', '1', '-process.alpha=2', '3']
        launcher.normalizeArgs('-x', '1', '-process.echo') == ['-x', '1', '-process.echo=true']
        launcher.normalizeArgs('-x', '1', '-process.echo', '-with-docker', 'ubuntu' ) == ['-x', '1', '-process.echo=true', '-with-docker','ubuntu']
        launcher.normalizeArgs('-x', '1', '-process.echo', '-123') == ['-x', '1', '-process.echo=-123' ]

        launcher.normalizeArgs('-x', '1', '-cluster.alpha','2', '3') == ['-x', '1', '-cluster.alpha=2', '3']
        launcher.normalizeArgs('-x', '1', '-cluster.echo') == ['-x', '1', '-cluster.echo=true']

        launcher.normalizeArgs('-x', '1', '-executor.alpha','2', '3') == ['-x', '1', '-executor.alpha=2', '3']
        launcher.normalizeArgs('-x', '1', '-executor.echo') == ['-x', '1', '-executor.echo=true']

        launcher.normalizeArgs('-x', '1', '-that.alpha','2', '3') == ['-x', '1', '-that.alpha','2', '3']

        launcher.normalizeArgs('run', 'file-name', '-a', '-b') == ['run','file-name', '-a', '-b']
        launcher.normalizeArgs('run', '-', '-a', '-b') == ['run','-stdin', '-a', '-b']
        launcher.normalizeArgs('run') == ['run']

        launcher.normalizeArgs('run','-with-drmaa') == ['run', '-with-drmaa','-']
        launcher.normalizeArgs('run','-with-drmaa', '-x') == ['run', '-with-drmaa','-', '-x']
        launcher.normalizeArgs('run','-with-drmaa', 'X') == ['run', '-with-drmaa','X']

        launcher.normalizeArgs('run','-with-trace') == ['run', '-with-trace','trace.txt']
        launcher.normalizeArgs('run','-with-trace', '-x') == ['run', '-with-trace','trace.txt', '-x']
        launcher.normalizeArgs('run','-with-trace', 'file.x') == ['run', '-with-trace','file.x']

        launcher.normalizeArgs('run','-with-report') == ['run', '-with-report','report.html']
        launcher.normalizeArgs('run','-with-report', '-x') == ['run', '-with-report','report.html', '-x']
        launcher.normalizeArgs('run','-with-report', 'file.x') == ['run', '-with-report','file.x']

        launcher.normalizeArgs('run','-with-timeline') == ['run', '-with-timeline','timeline.html']
        launcher.normalizeArgs('run','-with-timeline', '-x') == ['run', '-with-timeline','timeline.html', '-x']
        launcher.normalizeArgs('run','-with-timeline', 'file.x') == ['run', '-with-timeline','file.x']

        launcher.normalizeArgs('run','-with-dag') == ['run', '-with-dag','dag.dot']
        launcher.normalizeArgs('run','-with-dag', '-x') == ['run', '-with-dag','dag.dot', '-x']
        launcher.normalizeArgs('run','-with-dag', 'file.dot') == ['run', '-with-dag','file.dot']

        launcher.normalizeArgs('run','-with-docker') == ['run', '-with-docker','-']
        launcher.normalizeArgs('run','-with-docker', '-x') == ['run', '-with-docker','-', '-x']
        launcher.normalizeArgs('run','-with-docker', 'busybox') == ['run', '-with-docker','busybox']

        launcher.normalizeArgs('run','-with-singularity') == ['run', '-with-singularity','-']
        launcher.normalizeArgs('run','-with-singularity', '-x') == ['run', '-with-singularity','-', '-x']
        launcher.normalizeArgs('run','-with-singularity', 'busybox') == ['run', '-with-singularity','busybox']

        launcher.normalizeArgs('run','-dump-channels') == ['run', '-dump-channels','*']
        launcher.normalizeArgs('run','-dump-channels', '-x') == ['run', '-dump-channels','*', '-x']
        launcher.normalizeArgs('run','-dump-channels', 'foo,bar') == ['run', '-dump-channels','foo,bar']

        launcher.normalizeArgs('run','-with-notification', 'paolo@yo.com') == ['run', '-with-notification','paolo@yo.com']
        launcher.normalizeArgs('run','-with-notification') == ['run', '-with-notification','true']
        launcher.normalizeArgs('run','-with-notification', '-x') == ['run', '-with-notification','true', '-x']

        launcher.normalizeArgs('run','-N', 'paolo@yo.com') == ['run', '-N','paolo@yo.com']
        launcher.normalizeArgs('run','-N') == ['run', '-N','true']
        launcher.normalizeArgs('run','-N', '-x') == ['run', '-N','true', '-x']

        launcher.normalizeArgs('run','-K', 'true') == ['run', '-K','true']
        launcher.normalizeArgs('run','-K') == ['run', '-K','true']
        launcher.normalizeArgs('run','-K', '-x') == ['run', '-K','true', '-x']

        launcher.normalizeArgs('run','-with-k8s', 'true') == ['run', '-with-k8s','true']
        launcher.normalizeArgs('run','-with-k8s') == ['run', '-with-k8s','true']
        launcher.normalizeArgs('run','-with-k8s', '-x') == ['run', '-with-k8s','true', '-x']

        launcher.normalizeArgs('run','-syslog', 'host.com') == ['run', '-syslog','host.com']
        launcher.normalizeArgs('run','-syslog') == ['run', '-syslog','localhost']
        launcher.normalizeArgs('run','-syslog', '-x') == ['run', '-syslog','localhost', '-x']

        launcher.normalizeArgs( script.toAbsolutePath().toString(), '--x=1' ) == ['run', script.toAbsolutePath().toString(), '--x=1']


        cleanup:
        script?.delete()
    }


    def 'should parse proxy env variables'( ) {

        expect:
        Launcher.parseProxy(null) == []
        Launcher.parseProxy('http://domain') == ['domain']
        Launcher.parseProxy('http://domain:333') == ['domain', '333']
        Launcher.parseProxy('http://10.20.30.40') == ['10.20.30.40']
        Launcher.parseProxy('http://10.20.30.40:333') == ['10.20.30.40', '333']
        Launcher.parseProxy('http://10.20.30.40:333/some/path') == ['10.20.30.40', '333']

        Launcher.parseProxy('foo') == ['foo']
        Launcher.parseProxy('foo:123') == ['foo','123']

    }

    def 'should setup proxy properties'() {

        given:
        def httpProxyHost =System.getProperty('http.proxyHost')
        def httpProxyPort =System.getProperty('http.proxyPort')
        def httpsProxyHost =System.getProperty('https.proxyHost')
        def httpsProxyPort =System.getProperty('https.proxyPort')

        when:
        Launcher.setProxy('HTTP', [HTTP_PROXY: 'alpha.com:333'])
        then:
        System.getProperty('http.proxyHost') == 'alpha.com'
        System.getProperty('http.proxyPort') == '333'

        when:
        Launcher.setProxy('http', [http_proxy: 'gamma.com:444'])
        then:
        System.getProperty('http.proxyHost') == 'gamma.com'
        System.getProperty('http.proxyPort') == '444'

        when:
        Launcher.setProxy('HTTPS', [HTTPS_PROXY: 'beta.com:5466'])
        then:
        System.getProperty('https.proxyHost') == 'beta.com'
        System.getProperty('https.proxyPort') == '5466'

        when:
        Launcher.setProxy('https', [https_proxy: 'zeta.com:6646'])
        then:
        System.getProperty('https.proxyHost') == 'zeta.com'
        System.getProperty('https.proxyPort') == '6646'

        cleanup:
        System.setProperty('http.proxyHost', httpProxyHost ?: '')
        System.setProperty('http.proxyPort', httpProxyPort ?: '')
        System.setProperty('https.proxyHost', httpsProxyHost ?: '')
        System.setProperty('https.proxyPort', httpsProxyPort ?: '')
    }


    static class Sample {
        @CommandLine.Option(names = "--alpha") boolean alpha;
        @CommandLine.Parameters(index = '0', arity = '1') String name
        @CommandLine.Parameters(index = '1..*', arity = '*') List<String> params;
    }


    def 'should pass unknown parameters' () {
        given:
        def cmd = new CommandLine(new Sample())

        when:
        def sample = cmd.parse('--alpha', 'ciao', 'xx', '--yy').last().command as Sample
    
        then:
        sample.alpha
        sample.name == 'ciao'
        sample.params == ['xx','--yy']
    }


}
