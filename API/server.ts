import * as dotenv from 'dotenv'
dotenv.config()

import * as path from 'path'
import * as express from 'express'
import * as bodyParser from 'body-parser'
import * as aws from 'aws-sdk'
import * as multer from 'multer'
import * as multerS3 from 'multer-s3'
import * as mime from 'mime'

const app = express()
const port = process.env.PORT || 3000
const dev = process.env.PROD === 'false'

aws.config.update({ region: 'us-east-1' })
const ddb = new aws.DynamoDB({ apiVersion: '2012-10-08' })
const s3 = new aws.S3()

const upload = multer({
    storage: multerS3({
        s3,
        bucket: 'artisan-prof-pics',
        acl: 'public-read',
        metadata: (req, file, cb) => {
            cb(null, {
                fieldName: file.fieldname,
                'Content-Type': file.mimetype
            })
        },
        key: (req, file, cb) => {
            cb(
                null,
                Date.now().toString() + '.' + mime.getExtension(file.mimetype)
            )
        }
    })
})

const singleUpload = upload.single('image')

app.use(bodyParser.urlencoded({ extended: true }))
app.use(bodyParser.json())

app.listen(port, () => {
    console.log(`App is listening on port ${port}`)
})

app.get('/', (req: express.Request, res: express.Response) => {
    ddb.listTables({ Limit: 10 }, (err, data) => {
        console.log(data)
    })
    res.send('I did it!')
    ddb.listTables({ Limit: 10 }, (err, data) => {
        if (err) {
            console.log('Error', err.code)
        } else {
            console.log('Table names are ', data.TableNames)
        }
    })
    // res.sendFile(path.resolve(__dirname, 'frontEnd', 'index.html'))
})

const listAllArtisansParams: aws.DynamoDB.Types.QueryInput = {
    TableName: 'artisan',
    IndexName: 'cgoId-index',
    KeyConditionExpression: 'cgoId = :id',
    ExpressionAttributeValues: {
        ':id': { S: '0' }
    }
}

app.get('/artisans', (req: express.Request, res: express.Response) => {
    ddb.query(listAllArtisansParams, (err, data) => {
        if (err) {
            console.log(err)
            res.sendStatus(400)
        } else {
            const convert = data.Items.map(item => {
                return new Promise(resolve => {
                    resolve(aws.DynamoDB.Converter.unmarshall(item))
                })
            })
            Promise.all(convert).then(items => {
                res.json(items)
            })
        }
    })
})

app.get('/getDatabase', (req: express.Request, res: express.Response) => {
    res.json({ name: 'Mitchell' })
})

app.post(
    '/addArtisanToDatabase',
    (req: express.Request, res: express.Response) => {
        console.log(req.body)
        console.log(req.body.long)

        const params: aws.DynamoDB.PutItemInput = {
            TableName: 'artisan',
            Item: {
                artisanId: { S: req.body.artisanId },
                cgoId: { S: req.body.cgoId },
                bio: { S: req.body.bio },
                city: { S: req.body.city },
                country: { S: req.body.country },
                name: { S: req.body.name },
                lat: { N: req.body.lat },
                lon: { N: req.body.lon }
            }
        }
        ddb.putItem(params, (err, data) => {
            if (err) {
                console.log('Error', err.code)
                res.send(err.message)
                res.sendStatus(400)
            } else {
                console.log('Attributes ', data)
                res.send('Successfully added')
            }
        })
    }
)

app.post('/image-test', (req: express.Request, res: express.Response) => {
    singleUpload(req, res, err => {
        if (err) {
            return res.status(422).send({
                errors: [{ title: 'Image Upload Error', detail: err.message }]
            })
        }
        return res.json({ imageUrl: (req.file as any).location })
    })
})

// app.use(express.static(path.resolve(__dirname, 'frontEnd')))
