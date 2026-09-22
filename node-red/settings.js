module.exports = {
    uiPort: 1880,
    mqttReconnectTime: 15000,
    serialReconnectTime: 15000,
    debugMaxLength: 1000,
    adminAuth: false,
    https: false,
    flowFile: 'flows.json',
    credentialSecret: 'my-secret-key',
    flowFilePretty: true,
    userDir: './node-red',
    nodesDir: './node-red/nodes',
    paletteCategories: ['subflows', 'input', 'output', 'function', 'social', 'mobile', 'storage', 'analysis', 'advanced'],
    editorTheme: {
        projects: {
            enabled: false
        },
        header: {
            title: "Toyota TechMob - Node-RED"
        }
    },
    functionGlobalContext: {},
    contextStorage: {
        default: {
            module: 'memory'
        }
    },
    logging: {
        console: {
            level: 'info',
            metrics: false,
            audit: false
        }
    }
}
